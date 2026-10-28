package de.jeff_media.invunloadtest;

import de.jeff_media.InvUnload.Main;
import de.jeff_media.InvUnload.SelfTestRunner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Test-only player adapter. Worlds, containers, items, registries and events use the real Paper server. */
public final class CompatibilityProbe extends JavaPlugin {
    private final List<PermissionAttachment> permissions = new ArrayList<>();
    private final UUID playerId = UUID.randomUUID();
    private Location location;
    private ItemStack cursor = new ItemStack(Material.AIR);
    private int heldSlot;
    private boolean passed;
    private boolean failed;

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTask(this, this::verify);
    }

    private void verify() {
        try {
            String expectedVersion = System.getProperty("invunload.test.paperVersion");
            if(expectedVersion == null || !expectedVersion.equals(Bukkit.getMinecraftVersion())) {
                throw new IllegalStateException("Expected Paper " + expectedVersion + "; got " + Bukkit.getMinecraftVersion());
            }
            Main main = (Main) Bukkit.getPluginManager().getPlugin("InvUnload");
            if(main == null || !main.isEnabled()) throw new IllegalStateException("InvUnload did not enable.");
            if(!"1.21.11".equals(main.getDescription().getAPIVersion())) {
                throw new IllegalStateException("Unexpected minimum API: " + main.getDescription().getAPIVersion());
            }
            Inventory storage = Bukkit.createInventory(null, 36);
            Inventory equipment = Bukkit.createInventory(null, 9);
            Inventory enderChest = Bukkit.createInventory(null, 27);
            PlayerInventory inventory = inventory(storage, equipment);
            location = Bukkit.getWorlds().get(0).getSpawnLocation();
            Location originalLocation = location.clone();
            storage.setItem(12, new ItemStack(Material.EMERALD, 17));
            equipment.setItem(0, new ItemStack(Material.DIAMOND_BOOTS));
            enderChest.setItem(2, new ItemStack(Material.GOLD_INGOT, 9));
            cursor = new ItemStack(Material.APPLE, 3);
            heldSlot = 4;
            ItemStack[] originalInventory = inventory.getContents();
            Player player = (Player) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{Player.class},
                    (proxy, method, args) -> playerCall((Player) proxy, method, args, inventory, enderChest));
            var constructor = SelfTestRunner.class.getDeclaredConstructor(Main.class);
            constructor.setAccessible(true);
            constructor.newInstance(main).run(player);
            if(!passed || failed) throw new IllegalStateException("The feature self-test failed or did not finish.");
            if(!Arrays.equals(originalInventory, inventory.getContents())
                    || !new ItemStack(Material.GOLD_INGOT, 9).equals(enderChest.getItem(2))
                    || !new ItemStack(Material.APPLE, 3).equals(cursor)
                    || heldSlot != 4 || !location.equals(originalLocation)) {
                throw new IllegalStateException("Self-test did not restore the player's inventory, cursor or location.");
            }
            getLogger().info("COMPATIBILITY PASS: " + Bukkit.getVersion());
            Files.writeString(getServer().getWorldContainer().toPath().resolve("compatibility-result.txt"), "PASS\n");
        } catch (Throwable failure) {
            getLogger().severe("COMPATIBILITY FAIL: " + failure);
            failure.printStackTrace();
            try {
                Files.writeString(getServer().getWorldContainer().toPath().resolve("compatibility-result.txt"), "FAIL\n" + failure);
            } catch (Exception ignored) { }
        } finally {
            Bukkit.shutdown();
        }
    }

    private Object playerCall(Player player, Method method, Object[] args, PlayerInventory inventory, Inventory enderChest) {
        switch(method.getName()) {
            case "getUniqueId": return playerId;
            case "getName": case "getDisplayName": return "InvUnloadTest";
            case "getInventory": return inventory;
            case "getEnderChest": return enderChest;
            case "getItemOnCursor": return cursor;
            case "setItemOnCursor": cursor = ((ItemStack) args[0]).clone(); return null;
            case "getLocation":
                if(args == null || args.length == 0) return location.clone();
                Location target = (Location) args[0];
                target.setWorld(location.getWorld());
                target.setX(location.getX()); target.setY(location.getY()); target.setZ(location.getZ());
                target.setYaw(location.getYaw()); target.setPitch(location.getPitch());
                return target;
            case "getWorld": return location.getWorld();
            case "teleport": location = ((Location) args[0]).clone(); return true;
            case "getServer": return Bukkit.getServer();
            case "isOnline": case "isValid": case "isOp": return true;
            case "hasPermission":
                String name = args[0] instanceof Permission permission ? permission.getName() : (String) args[0];
                for(int index = permissions.size() - 1; index >= 0; index--) {
                    Boolean value = permissions.get(index).getPermissions().get(name);
                    if(value != null) return value;
                }
                return !name.startsWith("invunload.group.");
            case "addAttachment":
                PermissionAttachment attachment = new PermissionAttachment((org.bukkit.plugin.Plugin) args[0], player);
                permissions.add(attachment);
                return attachment;
            case "removeAttachment": permissions.remove(args[0]); return null;
            case "recalculatePermissions": return null;
            case "performCommand":
                String[] parts = ((String) args[0]).split(" +");
                PluginCommand command = Bukkit.getPluginCommand(parts[0]);
                if(command == null) throw new IllegalArgumentException("Missing command " + parts[0]);
                return command.execute(player, parts[0], Arrays.copyOfRange(parts, 1, parts.length));
            case "sendMessage":
                for(Object arg : args) {
                    if(arg instanceof String line) {
                        if(line.contains("Self-test completed successfully.")) passed = true;
                        if(line.contains("[FAIL]")) failed = true;
                    }
                }
                return null;
            case "sendActionBar": case "updateInventory": case "closeInventory": case "spawnParticle": case "playSound":
                return null;
            case "hashCode": return playerId.hashCode();
            case "equals": return player == args[0];
            case "toString": return "InvUnloadCompatibilityPlayer";
            default: throw new UnsupportedOperationException("Unimplemented player call: " + method);
        }
    }

    private PlayerInventory inventory(Inventory storage, Inventory equipment) {
        return (PlayerInventory) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> {
            switch(method.getName()) {
                case "getContents":
                    ItemStack[] contents = Arrays.copyOf(storage.getContents(), 41);
                    for(int slot = 0; slot < 5; slot++) contents[36 + slot] = equipment.getItem(slot);
                    return contents;
                case "setContents":
                    ItemStack[] items = (ItemStack[]) args[0];
                    storage.setContents(Arrays.copyOf(items, 36));
                    for(int slot = 0; slot < 5; slot++) equipment.setItem(slot, items[36 + slot]);
                    return null;
                case "clear":
                    if(args == null || args.length == 0) { storage.clear(); equipment.clear(); }
                    else if((int) args[0] < 36) storage.clear((int) args[0]);
                    else equipment.clear((int) args[0] - 36);
                    return null;
                case "getHeldItemSlot": return heldSlot;
                case "setHeldItemSlot": heldSlot = (int) args[0]; return null;
                case "getItemInMainHand":
                    ItemStack item = storage.getItem(heldSlot);
                    return item == null ? new ItemStack(Material.AIR) : item;
                default:
                    try { return Inventory.class.getMethod(method.getName(), method.getParameterTypes()).invoke(storage, args); }
                    catch (InvocationTargetException exception) { throw exception.getCause(); }
            }
        });
    }
}
