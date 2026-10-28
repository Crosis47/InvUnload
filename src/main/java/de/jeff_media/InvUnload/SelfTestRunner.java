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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import de.jeff_media.InvUnload.API.InvUnloadCheckAccessEvent;

import java.io.File;
import java.io.IOException;
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
        PlayerSnapshot playerSnapshot = new PlayerSnapshot(player, main);
        GroupUtils originalGroups = main.groupUtils;
        World testWorld = null;

        attachment.setPermission("invunload.use", true);
        attachment.setPermission("invunload.search", true);
        attachment.setPermission("invunload.search.take", true);
        attachment.setPermission("invunload.selftest", true);

        try {
            player.closeInventory();
            main.playerSettings.put(player.getUniqueId(), new PlayerSetting());
            // Group overrides must not change the fixture's radii.
            main.groupUtils = new GroupUtils(main, new File(main.getDataFolder(), "selftest-unused-groups.yml"));
            main.groupUtils.yaml = null;
            prepareConfig();

            World world = prepareWorld();
            testWorld = world;
            if(!player.teleport(new Location(world, 0.5, TEST_Y + 1, 4.5, 180f, 0f))) {
                throw new IllegalStateException("Teleport to self-test world was denied.");
            }
            player.getInventory().clear();
            player.getEnderChest().clear();
            clearPlayerState(player);

            runSearchTest(player, world, report);
            runEnchantmentSearchTest(player, world, report);
            runTakeSafeguardsTest(player, world, attachment, report);
            runUnloadTest(player, world, report);
            runDumpTest(player, world, report);
            runBlacklistTest(player, world, report);
            runLockedSlotsTest(player, world, report);
        } catch (Exception exception) {
            report.fail("Harness setup", exception.getClass().getSimpleName() + ": " + exception.getMessage());
            main.getLogger().warning("InvUnload self-test failed with an exception.");
            exception.printStackTrace();
        } finally {
            configSnapshot.restore(main.getConfig());
            main.groupUtils = originalGroups;
            attachment.remove();
            try {
                if(testWorld != null) resetContainers(testWorld);
            } finally {
                playerSnapshot.restore(player, main);
            }
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
        config.set("max-search-radius", TEST_RADIUS);
        config.set("default-search-radius", TEST_RADIUS);
        config.set("search-take-radius", TEST_RADIUS);
        config.set("search-take-links", true);
        config.set("use-playerinteractevent", true);
        config.set("max-chest-radius", TEST_RADIUS);
    }

    private World prepareWorld() {
        World world = Bukkit.getWorld(TEST_WORLD_NAME);
        if (world == null) {
            WorldCreator creator = new WorldCreator(TEST_WORLD_NAME);
            creator.type(WorldType.FLAT);
            creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:stone\",\"height\":2}],\"biome\":\"minecraft:plains\"}");
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
        chestInventory(world, 6, TEST_Y, 0).addItem(new ItemStack(Material.DIAMOND, 1));
        main.getConfig().set("default-search-radius", 5);

        player.performCommand("searchitem DIAMOND");

        ArrayList<org.bukkit.block.Block> affected = main.visualizer.lastUnloads.get(player.getUniqueId());
        boolean foundChest = wasChestHighlighted(affected, world, 0);
        report.check("searchitem finds matching chest", foundChest,
                foundChest ? "Found DIAMOND chest at expected location." : "Expected DIAMOND chest was not highlighted.");
        report.check("searchitem uses the configured default radius", foundChest && !wasChestHighlighted(affected, world, 6),
                "Nearby chest found; a chest outside the five-block default was excluded.");
        main.getConfig().set("default-search-radius", TEST_RADIUS);

        UnloadSummary summary = main.visualizer.unloadSummaries.get(player.getUniqueId());
        List<Integer> slots = summary == null ? new ArrayList<>() : summary.getSearchSlots(new Location(world, 0, TEST_Y, 0), Material.DIAMOND);
        report.check("searchitem reports matching chest slot",
                slots.size() == 1 && slots.contains(1),
                "Reported DIAMOND slots: " + slots);

        List<UnloadSummary.SearchSlot> slotEntries = summary == null ? new ArrayList<>() : summary.getSearchSlotEntries(new Location(world, 0, TEST_Y, 0), Material.DIAMOND);
        String takeCommand = slotEntries.isEmpty() ? null : slotEntries.get(0).getTakeCommand();
        ItemStack hoverSnapshot = slotEntries.isEmpty() ? null : slotEntries.get(0).getItemSnapshot();
        report.check("searchitem take link keeps item tooltip data",
                hoverSnapshot != null && hoverSnapshot.isSimilar(new ItemStack(Material.DIAMOND)),
                "Tooltip snapshot: " + (hoverSnapshot == null ? "none" : hoverSnapshot.getType().name()));

        if(takeCommand != null && takeCommand.startsWith("/")) {
            player.performCommand(takeCommand.substring(1));
        }

        int playerDiamonds = countPlayerInventory(player, Material.DIAMOND);
        int chestDiamonds = count(matchingChest, Material.DIAMOND);
        report.check("searchitem take link moves matching stack",
                playerDiamonds == 32 && chestDiamonds == 0,
                "Player diamonds: " + playerDiamonds + ", chest diamonds: " + chestDiamonds);
    }

    private void runEnchantmentSearchTest(Player player, World world, TestReport report) {
        resetContainers(world);
        clearPlayerState(player);

        Enchantment mending = SearchItemQuery.parseEnchantment("mending");
        if(mending == null) {
            report.fail("searchitem finds enchanted items", "MENDING enchantment was not available.");
            return;
        }

        Inventory matchingChest = chestInventory(world, 0, TEST_Y, 0);
        matchingChest.addItem(enchantedItem(Material.DIAMOND_PICKAXE, mending));

        Inventory plainChest = chestInventory(world, 3, TEST_Y, 0);
        plainChest.addItem(new ItemStack(Material.DIAMOND_PICKAXE));

        Inventory bookChest = chestInventory(world, 6, TEST_Y, 0);
        bookChest.addItem(enchantedBook(mending));

        player.performCommand("searchitem MENDING " + TEST_RADIUS);

        ArrayList<org.bukkit.block.Block> affected = main.visualizer.lastUnloads.get(player.getUniqueId());
        boolean foundEnchantedItem = wasChestHighlighted(affected, world, 0);
        boolean ignoredPlainItem = !wasChestHighlighted(affected, world, 3);
        boolean foundEnchantedBook = wasChestHighlighted(affected, world, 6);

        report.check("searchitem finds enchanted items",
                foundEnchantedItem && ignoredPlainItem && foundEnchantedBook,
                "Item chest: " + foundEnchantedItem + ", plain chest ignored: " + ignoredPlainItem + ", book chest: " + foundEnchantedBook);

        main.visualizer.lastUnloads.remove(player.getUniqueId());
        player.performCommand("searchitem DIAMOND_PICKAXE MENDING " + TEST_RADIUS);
        affected = main.visualizer.lastUnloads.get(player.getUniqueId());
        report.check("searchitem combines material and enchantment filters",
                wasChestHighlighted(affected, world, 0) && !wasChestHighlighted(affected, world, 3)
                        && !wasChestHighlighted(affected, world, 6),
                "Only the Mending pickaxe chest should match.");
    }

    private void runTakeSafeguardsTest(Player player, World world, PermissionAttachment attachment, TestReport report) {
        resetContainers(world);
        clearPlayerState(player);
        Inventory chest = chestInventory(world, 0, TEST_Y, 0);
        Location chestLocation = new Location(world, 0, TEST_Y, 0);
        chest.setItem(0, new ItemStack(Material.DIAMOND, 32));
        String take = main.commandSearchitem.createTakeCommand(player, chestLocation, 0, chest.getItem(0));

        Location nearby = player.getLocation();
        player.teleport(new Location(world, 30, TEST_Y + 1, 0));
        executeTake(player, take);
        report.check("take link enforces distance", count(chest, Material.DIAMOND) == 32
                && countPlayerInventory(player, Material.DIAMOND) == 0, "Distant take leaves the stack intact.");
        player.teleport(nearby);

        main.getConfig().set("search-take-links", false);
        executeTake(player, take);
        report.check("take link respects disabled configuration", count(chest, Material.DIAMOND) == 32
                && countPlayerInventory(player, Material.DIAMOND) == 0, "Existing link cannot bypass configuration.");
        main.getConfig().set("search-take-links", true);

        attachment.setPermission("invunload.search.take", false);
        executeTake(player, take);
        report.check("take link rechecks permission", count(chest, Material.DIAMOND) == 32
                && countPlayerInventory(player, Material.DIAMOND) == 0, "Revoked permission prevents taking.");
        attachment.setPermission("invunload.search.take", true);

        Listener denyAccess = new Listener() {
            @EventHandler
            public void onAccess(InvUnloadCheckAccessEvent event) {
                if(event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    event.setUseInteractedBlock(Event.Result.DENY);
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(denyAccess, main);
        try {
            executeTake(player, take);
            report.check("take link rechecks chest access", count(chest, Material.DIAMOND) == 32
                    && countPlayerInventory(player, Material.DIAMOND) == 0, "Protection denial leaves the stack intact.");
        } finally {
            HandlerList.unregisterAll(denyAccess);
        }

        chest.setItem(0, new ItemStack(Material.EMERALD, 7));
        executeTake(player, take);
        report.check("take link rejects a replaced stack", count(chest, Material.EMERALD) == 7
                && countPlayerInventory(player, Material.EMERALD) == 0, "A stale result cannot take a replacement.");

        chest.setItem(0, new ItemStack(Material.DIAMOND, 32));
        take = main.commandSearchitem.createTakeCommand(player, chestLocation, 0, chest.getItem(0));
        for(int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        executeTake(player, take);
        report.check("take link handles a full inventory", count(chest, Material.DIAMOND) == 32
                && countPlayerInventory(player, Material.DIAMOND) == 0, "Full inventory leaves the chest unchanged.");
        player.getInventory().setItem(9, new ItemStack(Material.DIAMOND, 60));
        executeTake(player, take);
        report.check("take link preserves partial-transfer leftovers", count(chest, Material.DIAMOND) == 28
                && countPlayerInventory(player, Material.DIAMOND) == 64, "Moved four diamonds; 28 remain in the chest.");
        player.getInventory().clear();
        executeTake(player, take);
        report.check("take link cannot be replayed", count(chest, Material.DIAMOND) == 28
                && countPlayerInventory(player, Material.DIAMOND) == 0, "Used token cannot move another stack.");

        take = main.commandSearchitem.createTakeCommand(player, chestLocation, 0, chest.getItem(0));
        main.getConfig().set("search-take-radius", -1);
        World otherWorld = Bukkit.getWorlds().stream().filter(candidate -> !candidate.equals(world)).findFirst().orElseThrow();
        player.teleport(otherWorld.getSpawnLocation());
        executeTake(player, take);
        report.check("unlimited take distance still requires the same world", count(chest, Material.DIAMOND) == 28
                && countPlayerInventory(player, Material.DIAMOND) == 0, "Cross-world take was rejected.");
        player.teleport(nearby);
        main.getConfig().set("search-take-radius", TEST_RADIUS);
    }

    private void executeTake(Player player, String command) {
        if(command == null) throw new IllegalStateException("Expected a take command.");
        player.performCommand(command.substring(1));
    }

    private void runLockedSlotsTest(Player player, World world, TestReport report) throws IOException {
        for(String command : List.of("unload", "dump")) {
            resetContainers(world);
            clearPlayerState(player);
            Inventory chest = chestInventory(world, 0, TEST_Y, 0);
            chest.setItem(0, new ItemStack(Material.DIAMOND, 1));
            PlayerSetting setting = main.getPlayerSetting(player);
            setting.unloadHotbar = true;
            setting.dumpHotbar = true;
            setting.setSlotLocked(0, true);
            setting.setSlotLocked(9, true);
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 2));
            player.getInventory().setItem(9, new ItemStack(Material.DIAMOND, 3));
            player.getInventory().setItem(10, new ItemStack(Material.DIAMOND, 5));
            player.performCommand(command + " " + TEST_RADIUS);
            report.check(command + " skips locked hotbar and inventory slots",
                    countPlayerInventory(player, Material.DIAMOND) == 5 && count(chest, Material.DIAMOND) == 6
                            && player.getInventory().getItem(0).getAmount() == 2
                            && player.getInventory().getItem(9).getAmount() == 3,
                    "Locked stacks remain; the unlocked stack moves.");
        }

        PlayerSetting setting = main.getPlayerSetting(player);
        ItemStack original = player.getInventory().getItem(9).clone();
        LockedSlotsMenu menu = new LockedSlotsMenu(main, player);
        ItemStack preview = menu.createSlotPreview(9);
        report.check("locked-slot preview preserves the actual item",
                player.getInventory().getItem(9).equals(original)
                        && Boolean.TRUE.equals(preview.getItemMeta().getEnchantmentGlintOverride()),
                "Preview glint does not modify inventory contents.");
        File fixture = File.createTempFile("selftest-slots-", ".yml", main.getDataFolder());
        try {
            setting.save(fixture, main);
            PlayerSetting restored = new PlayerSetting(fixture);
            report.check("locked slots survive save and reload", restored.lockedSlots.equals(setting.lockedSlots)
                    && restored.unloadHotbar && restored.dumpHotbar, "Persisted locked slots: " + restored.lockedSlots);
        } finally {
            java.nio.file.Files.deleteIfExists(fixture.toPath());
        }
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
        main.visualizer.cancelVisualization(player);
        main.visualizer.lastUnloadPositions.remove(player.getUniqueId());

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

    private boolean wasChestHighlighted(ArrayList<org.bukkit.block.Block> affected, World world, int x) {
        return affected != null && affected.stream().anyMatch(block -> block.getLocation().equals(new Location(world, x, TEST_Y, 0)));
    }

    private ItemStack enchantedItem(Material material, Enchantment enchantment) {
        ItemStack item = new ItemStack(material);
        item.addUnsafeEnchantment(enchantment, 1);
        return item;
    }

    private ItemStack enchantedBook(Enchantment enchantment) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(enchantment, 1, true);
        item.setItemMeta(meta);
        return item;
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
            snapshot(config, "max-search-radius");
            snapshot(config, "default-search-radius");
            snapshot(config, "search-take-radius");
            snapshot(config, "search-take-links");
            snapshot(config, "use-playerinteractevent");
            snapshot(config, "max-chest-radius");
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

    private static class PlayerSnapshot {
        private final Location location;
        private final ItemStack[] inventory;
        private final ItemStack[] enderChest;
        private final ItemStack cursor;
        private final int heldSlot;
        private final PlayerSetting setting;
        private final ArrayList<org.bukkit.block.Block> lastUnloads;
        private final Location lastUnloadPosition;
        private final UnloadSummary summary;

        private PlayerSnapshot(Player player, Main main) {
            location = player.getLocation().clone();
            inventory = cloneContents(player.getInventory());
            enderChest = cloneContents(player.getEnderChest());
            cursor = player.getItemOnCursor().clone();
            heldSlot = player.getInventory().getHeldItemSlot();
            setting = main.getPlayerSetting(player);
            UUID id = player.getUniqueId();
            lastUnloads = main.visualizer.lastUnloads.get(id);
            lastUnloadPosition = main.visualizer.lastUnloadPositions.get(id);
            summary = main.visualizer.unloadSummaries.get(id);
        }

        private static ItemStack[] cloneContents(Inventory source) {
            ItemStack[] contents = source.getContents();
            for(int slot = 0; slot < contents.length; slot++) {
                if(contents[slot] != null) contents[slot] = contents[slot].clone();
            }
            return contents;
        }

        private void restore(Player player, Main main) {
            UUID id = player.getUniqueId();
            main.visualizer.cancelVisualization(player);
            main.playerSettings.put(id, setting);
            restoreEntry(main.visualizer.lastUnloads, id, lastUnloads);
            restoreEntry(main.visualizer.lastUnloadPositions, id, lastUnloadPosition);
            restoreEntry(main.visualizer.unloadSummaries, id, summary);
            player.getInventory().setContents(inventory);
            player.getEnderChest().setContents(enderChest);
            player.getInventory().setHeldItemSlot(heldSlot);
            player.setItemOnCursor(cursor);
            if(!player.teleport(location)) main.getLogger().warning("Could not return self-test player to their original location.");
            player.updateInventory();
        }

        private static <T> void restoreEntry(Map<UUID, T> map, UUID id, T value) {
            if(value == null) map.remove(id);
            else map.put(id, value);
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
