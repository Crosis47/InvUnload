package de.jeff_media.InvUnload;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LockedSlotsMenu implements InventoryHolder {

    private static final String TITLE = ChatColor.DARK_GREEN + "Ignored Slots";
    private static final int[] HOTBAR_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] INVENTORY_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35
    };

    private final Main main;
    private final Player player;
    private final Inventory inventory;

    LockedSlotsMenu(Main main, Player player) {
        this.main = main;
        this.player = player;
        inventory = Bukkit.createInventory(this, 45, TITLE);
        refresh();
    }

    void open() {
        refresh();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void handleClick(int rawSlot) {
        PlayerSetting setting = main.getPlayerSetting(player);
        switch (rawSlot) {
            case 2 -> {
                setting.setSlotsLocked(asList(HOTBAR_SLOTS), true);
                persist(setting);
                player.sendActionBar(ChatColor.RED + "Locked all hotbar slots.");
            }
            case 3 -> {
                setting.setSlotsLocked(asList(HOTBAR_SLOTS), false);
                persist(setting);
                player.sendActionBar(ChatColor.GREEN + "Unlocked all hotbar slots.");
            }
            case 5 -> {
                setting.setSlotsLocked(asList(INVENTORY_SLOTS), true);
                persist(setting);
                player.sendActionBar(ChatColor.RED + "Locked all inventory slots.");
            }
            case 6 -> {
                setting.setSlotsLocked(asList(INVENTORY_SLOTS), false);
                persist(setting);
                player.sendActionBar(ChatColor.GREEN + "Unlocked all inventory slots.");
            }
            case 8 -> {
                setting.clearLockedSlots();
                persist(setting);
                player.sendActionBar(ChatColor.GREEN + "Cleared all ignored slots.");
            }
            default -> {
                int playerSlot = playerSlotFromMenuSlot(rawSlot);
                if(playerSlot == -1) return;
                setting.toggleLockedSlot(playerSlot);
                persist(setting);
                boolean locked = setting.isSlotLocked(playerSlot);
                player.sendActionBar((locked ? ChatColor.RED : ChatColor.GREEN) + (locked ? "Locked " : "Unlocked ") + describeSlot(playerSlot) + ".");
            }
        }
        refresh();
    }

    void refresh() {
        inventory.clear();

        for(int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, createFiller());
        }

        inventory.setItem(0, createButton(
                Material.BOOK,
                ChatColor.YELLOW + "How it works",
                ChatColor.GRAY + "Click any slot below to toggle it.",
                ChatColor.GRAY + "Locked slots are ignored by",
                ChatColor.GRAY + "/unload and /dump."
        ));
        inventory.setItem(2, createButton(
                Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Lock hotbar",
                ChatColor.GRAY + "Ignore hotbar slots 1-9."
        ));
        inventory.setItem(3, createButton(
                Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Unlock hotbar",
                ChatColor.GRAY + "Re-enable hotbar slots 1-9."
        ));
        inventory.setItem(5, createButton(
                Material.REDSTONE_BLOCK,
                ChatColor.RED + "Lock inventory",
                ChatColor.GRAY + "Ignore inventory slots 10-36."
        ));
        inventory.setItem(6, createButton(
                Material.EMERALD_BLOCK,
                ChatColor.GREEN + "Unlock inventory",
                ChatColor.GRAY + "Re-enable inventory slots 10-36."
        ));
        inventory.setItem(8, createButton(
                Material.BARRIER,
                ChatColor.GOLD + "Clear all ignored slots",
                ChatColor.GRAY + "Reset every slot back to normal."
        ));

        for(int menuSlot = 9; menuSlot < inventory.getSize(); menuSlot++) {
            int playerSlot = playerSlotFromMenuSlot(menuSlot);
            if(playerSlot == -1) continue;
            inventory.setItem(menuSlot, createSlotPreview(playerSlot));
        }
    }

    private void persist(PlayerSetting setting) {
        setting.save(main.getPlayerFile(player.getUniqueId()), main);
    }

    private ItemStack createSlotPreview(int playerSlot) {
        ItemStack current = player.getInventory().getItem(playerSlot);
        boolean locked = main.getPlayerSetting(player).isSlotLocked(playerSlot);

        if(current == null || current.getType() == Material.AIR) {
            Material material = locked ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            return createButton(
                    material,
                    (locked ? ChatColor.RED : ChatColor.GRAY) + describeSlot(playerSlot),
                    locked
                            ? ChatColor.RED + "Ignored by /unload and /dump."
                            : ChatColor.GREEN + "Click to ignore this slot."
            );
        }

        ItemStack preview = current.clone();
        ItemMeta meta = preview.getItemMeta();
        if(meta == null) {
            return preview;
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + describeSlot(playerSlot));
        lore.add((locked ? ChatColor.RED : ChatColor.GREEN) + (locked ? "Ignored by /unload and /dump" : "Included in /unload and /dump"));
        lore.add(ChatColor.YELLOW + "Click to toggle");
        meta.setLore(lore);

        if(locked) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        preview.setItemMeta(meta);
        return preview;
    }

    private ItemStack createButton(Material material, String name, String... loreLines) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if(meta == null) {
            return itemStack;
        }
        meta.setDisplayName(name);
        meta.setLore(List.of(loreLines));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack createFiller() {
        return createButton(Material.BLACK_STAINED_GLASS_PANE, " ", " ");
    }

    private int playerSlotFromMenuSlot(int menuSlot) {
        if(menuSlot >= 9 && menuSlot <= 35) {
            return menuSlot;
        }
        if(menuSlot >= 36 && menuSlot <= 44) {
            return menuSlot - 36;
        }
        return -1;
    }

    private String describeSlot(int slot) {
        if(slot <= 8) {
            return "Hotbar slot " + (slot + 1);
        }
        return "Inventory slot " + (slot - 8);
    }

    private List<Integer> asList(int[] slots) {
        List<Integer> list = new ArrayList<>();
        for(int slot : slots) {
            list.add(slot);
        }
        return list;
    }
}
