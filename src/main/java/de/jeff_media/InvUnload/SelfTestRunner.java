package de.jeff_media.InvUnload;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SelfTestRunner {

    private static final String TEST_WORLD_NAME = "invunload-selftest";
    private static final int TEST_Y = 100;
    private static final int TEST_RADIUS = 10;

    private final Main main;

    SelfTestRunner(Main main) {
        this.main = main;
    }

    public void run(Player player) {
        TestReport report = new TestReport(player.getUniqueId());
        PermissionAttachment attachment = player.addAttachment(main);
        ConfigSnapshot configSnapshot = new ConfigSnapshot(main.getConfig());

        attachment.setPermission("invunload.use", true);
        attachment.setPermission("invunload.search", true);
        attachment.setPermission("invunload.selftest", true);

        try {
            prepareConfig();

            World world = prepareWorld();
            player.teleport(new Location(world, 0.5, TEST_Y + 1, 4.5, 180f, 0f));
            player.getInventory().clear();
            player.getEnderChest().clear();
            clearPlayerState(player);

            runSearchTest(player, world, report);
            runUnloadTest(player, world, report);
            runDumpTest(player, world, report);
            runBlacklistTest(player, world, report);
        } catch (Exception exception) {
            report.fail("Harness setup", exception.getClass().getSimpleName() + ": " + exception.getMessage());
            main.getLogger().warning("InvUnload self-test failed with an exception.");
            exception.printStackTrace();
        } finally {
            configSnapshot.restore(main.getConfig());
            attachment.remove();
            clearPlayerState(player);
        }

        report.print(player, main);
    }

    private void prepareConfig() {
        FileConfiguration config = main.getConfig();
        config.set("debug", true);
        config.set("cooldown", 0);
        config.set("spawn-particles", false);
        config.set("play-sound", false);
        config.set("laser-animation", false);
        config.set("always-show-summary", false);
    }

    private World prepareWorld() {
        World world = Bukkit.getWorld(TEST_WORLD_NAME);
        if (world == null) {
            WorldCreator creator = new WorldCreator(TEST_WORLD_NAME);
            creator.type(WorldType.FLAT);
            world = creator.createWorld();
        }

        if (world == null) {
            throw new IllegalStateException("Could not create test world.");
        }

        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);

        clearArena(world);
        buildArena(world);
        world.getChunkAt(0, 0).load();
        return world;
    }

    private void clearArena(World world) {
        for (int x = -4; x <= 8; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = TEST_Y - 1; y <= TEST_Y + 3; y++) {
                    Material type = y == TEST_Y - 1 ? Material.STONE : Material.AIR;
                    world.getBlockAt(x, y, z).setType(type, false);
                }
            }
        }
    }

    private void buildArena(World world) {
        world.getBlockAt(0, TEST_Y, 0).setType(Material.CHEST, false);
        world.getBlockAt(3, TEST_Y, 0).setType(Material.CHEST, false);
        world.getBlockAt(6, TEST_Y, 0).setType(Material.CHEST, false);
    }

    private void runSearchTest(Player player, World world, TestReport report) {
        resetContainers(world);
        clearPlayerState(player);

        Inventory matchingChest = chestInventory(world, 0, TEST_Y, 0);
        matchingChest.addItem(new ItemStack(Material.DIAMOND, 32));

        player.performCommand("searchitem DIAMOND " + TEST_RADIUS);

        ArrayList<org.bukkit.block.Block> affected = main.visualizer.lastUnloads.get(player.getUniqueId());
        boolean foundChest = affected != null && affected.stream().anyMatch(block -> block.getLocation().equals(new Location(world, 0, TEST_Y, 0)));
        report.check("searchitem finds matching chest", foundChest,
                foundChest ? "Found DIAMOND chest at expected location." : "Expected DIAMOND chest was not highlighted.");
    }

    private void runUnloadTest(Player player, World world, TestReport report) {
        resetContainers(world);
        clearPlayerState(player);

        Inventory matchingChest = chestInventory(world, 0, TEST_Y, 0);
        matchingChest.addItem(new ItemStack(Material.DIAMOND, 32));
        int beforeDiamonds = count(matchingChest, Material.DIAMOND);

        player.getInventory().setItem(9, new ItemStack(Material.DIAMOND, 5));
        player.getInventory().setItem(10, new ItemStack(Material.STONE, 7));

        player.performCommand("unload " + TEST_RADIUS);

        int afterDiamonds = count(matchingChest, Material.DIAMOND);
        int playerDiamonds = countPlayerInventory(player, Material.DIAMOND);
        int playerStone = countPlayerInventory(player, Material.STONE);

        report.check("unload moves only matching items",
                afterDiamonds == beforeDiamonds + 5 && playerDiamonds == 0 && playerStone == 7,
                "Chest diamonds: " + afterDiamonds + ", player diamonds: " + playerDiamonds + ", player stone: " + playerStone);
    }

    private void runDumpTest(Player player, World world, TestReport report) {
        resetContainers(world);
        clearPlayerState(player);

        Inventory matchingChest = chestInventory(world, 0, TEST_Y, 0);
        Inventory emptyChest = chestInventory(world, 3, TEST_Y, 0);
        matchingChest.addItem(new ItemStack(Material.DIAMOND, 32));

        player.getInventory().setItem(9, new ItemStack(Material.DIAMOND, 5));
        player.getInventory().setItem(10, new ItemStack(Material.STONE, 7));

        player.performCommand("dump " + TEST_RADIUS);

        int diamondsAcrossChests = count(matchingChest, Material.DIAMOND) + count(emptyChest, Material.DIAMOND);
        int stoneAcrossChests = count(matchingChest, Material.STONE) + count(emptyChest, Material.STONE);
        int playerDiamonds = countPlayerInventory(player, Material.DIAMOND);
        int playerStone = countPlayerInventory(player, Material.STONE);

        report.check("dump unloads matches then dumps leftovers",
                diamondsAcrossChests == 37 && stoneAcrossChests == 7 && playerDiamonds == 0 && playerStone == 0,
                "Chest diamonds: " + diamondsAcrossChests + ", chest stone: " + stoneAcrossChests + ", player diamonds: " + playerDiamonds + ", player stone: " + playerStone);
    }

    private void runBlacklistTest(Player player, World world, TestReport report) {
        resetContainers(world);
        clearPlayerState(player);

        PlayerSetting setting = main.getPlayerSetting(player);
        setting.blacklist.mats.clear();
        setting.blacklist.add(Material.STONE);

        Inventory matchingChest = chestInventory(world, 0, TEST_Y, 0);
        Inventory emptyChest = chestInventory(world, 3, TEST_Y, 0);
        player.getInventory().setItem(9, new ItemStack(Material.STONE, 7));

        player.performCommand("dump " + TEST_RADIUS);

        int playerStone = countPlayerInventory(player, Material.STONE);
        int chestStone = count(matchingChest, Material.STONE) + count(emptyChest, Material.STONE);

        report.check("blacklist prevents dump/unload for blacklisted items",
                playerStone == 7 && chestStone == 0,
                "Player stone: " + playerStone + ", chest stone: " + chestStone);

        setting.blacklist.mats.clear();
    }

    private void resetContainers(World world) {
        chestInventory(world, 0, TEST_Y, 0).clear();
        chestInventory(world, 3, TEST_Y, 0).clear();
        chestInventory(world, 6, TEST_Y, 0).clear();
    }

    private void clearPlayerState(Player player) {
        player.getInventory().clear();
        main.visualizer.lastUnloads.remove(player.getUniqueId());
        main.visualizer.unloadSummaries.remove(player.getUniqueId());
        main.visualizer.activeVisualizations.remove(player.getUniqueId());

        PlayerSetting setting = main.getPlayerSetting(player);
        setting.unloadHotbar = false;
        setting.dumpHotbar = false;
        setting.blacklist.mats.clear();
        setting.clearLockedSlots();
    }

    private Inventory chestInventory(World world, int x, int y, int z) {
        return ((Chest) world.getBlockAt(x, y, z).getState()).getInventory();
    }

    private int count(Inventory inventory, Material material) {
        int total = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.getType() == material) {
                total += itemStack.getAmount();
            }
        }
        return total;
    }

    private int countPlayerInventory(Player player, Material material) {
        return count(player.getInventory(), material);
    }

    private static class ConfigSnapshot {

        private final Map<String, Object> values = new LinkedHashMap<>();

        private ConfigSnapshot(FileConfiguration config) {
            snapshot(config, "debug");
            snapshot(config, "cooldown");
            snapshot(config, "spawn-particles");
            snapshot(config, "play-sound");
            snapshot(config, "laser-animation");
            snapshot(config, "always-show-summary");
        }

        private void snapshot(FileConfiguration config, String path) {
            values.put(path, config.get(path));
        }

        private void restore(FileConfiguration config) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private static class TestReport {

        private final UUID playerId;
        private final List<String> lines = new ArrayList<>();
        private boolean hasFailures;

        private TestReport(UUID playerId) {
            this.playerId = playerId;
        }

        private void check(String name, boolean success, String detail) {
            if (success) {
                lines.add(ChatColor.GREEN + "[PASS] " + ChatColor.RESET + name + " - " + detail);
            } else {
                fail(name, detail);
            }
        }

        private void fail(String name, String detail) {
            hasFailures = true;
            lines.add(ChatColor.RED + "[FAIL] " + ChatColor.RESET + name + " - " + detail);
        }

        private void print(Player player, Main main) {
            player.sendMessage(ChatColor.AQUA + "InvUnload self-test results");
            for (String line : lines) {
                player.sendMessage(line);
                main.getLogger().info(ChatColor.stripColor("[SelfTest] " + line));
            }
            String summary = hasFailures
                    ? ChatColor.RED + "Self-test completed with failures."
                    : ChatColor.GREEN + "Self-test completed successfully.";
            player.sendMessage(summary);
            main.getLogger().info(ChatColor.stripColor("[SelfTest] " + summary + " Player=" + playerId));
        }
    }
}
