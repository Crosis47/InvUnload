package de.jeff_media.InvUnload;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.ListenerPriority;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

interface LockedSlotsPreview {

    void show(LockedSlotsMenu menu);

    void showSlot(LockedSlotsMenu menu, int playerSlot);

    void restore(Player player);

    void restoreAll();
}

class ProtocolLibLockedSlotsPreview implements LockedSlotsPreview {

    private static final int FALLBACK_PLAYER_INVENTORY_WINDOW_ID = -2;

    private final Main main;
    private final ProtocolManager protocolManager;
    private final Map<UUID, LockedSlotsMenu> activeMenus = new HashMap<>();
    private final Map<UUID, Integer> openWindowIds = new HashMap<>();
    private final Set<UUID> viewers = new HashSet<>();
    private boolean disabled;

    ProtocolLibLockedSlotsPreview(Main main) {
        this.main = main;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        registerWindowTracker();
        registerInventoryCorrectionRewriter();
    }

    @Override
    public void show(LockedSlotsMenu menu) {
        if(disabled) {
            return;
        }

        Player player = menu.getPlayer();
        activeMenus.put(player.getUniqueId(), menu);
        viewers.add(player.getUniqueId());

        for(int playerSlot = 9; playerSlot <= 35; playerSlot++) {
            if(disabled) return;
            sendSlot(player, playerSlot, menu.createPlayerInventoryPreview(playerSlot));
        }
        for(int playerSlot = 0; playerSlot <= 8; playerSlot++) {
            if(disabled) return;
            sendSlot(player, playerSlot, menu.createPlayerInventoryPreview(playerSlot));
        }
    }

    @Override
    public void showSlot(LockedSlotsMenu menu, int playerSlot) {
        if(disabled) {
            return;
        }

        Player player = menu.getPlayer();
        activeMenus.put(player.getUniqueId(), menu);
        viewers.add(player.getUniqueId());
        sendSlot(player, playerSlot, menu.createPlayerInventoryPreview(playerSlot));
    }

    @Override
    public void restore(Player player) {
        activeMenus.remove(player.getUniqueId());
        viewers.remove(player.getUniqueId());
        if(!disabled) {
            for(int playerSlot = 9; playerSlot <= 35; playerSlot++) {
                if(disabled) break;
                sendSlot(player, playerSlot, player.getInventory().getItem(playerSlot));
            }
            for(int playerSlot = 0; playerSlot <= 8; playerSlot++) {
                if(disabled) break;
                sendSlot(player, playerSlot, player.getInventory().getItem(playerSlot));
            }
        }

        player.updateInventory();
        if(main.isEnabled()) {
            Bukkit.getScheduler().runTask(main, player::updateInventory);
        }
        openWindowIds.remove(player.getUniqueId());
    }

    @Override
    public void restoreAll() {
        for(UUID uuid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(uuid);
            if(player != null) {
                restore(player);
            }
        }
        viewers.clear();
    }

    private void registerWindowTracker() {
        protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.MONITOR, PacketType.Play.Server.OPEN_WINDOW) {
            @Override
            public void onPacketSending(PacketEvent event) {
                openWindowIds.put(event.getPlayer().getUniqueId(), event.getPacket().getIntegers().read(0));
            }
        });
    }

    private void registerInventoryCorrectionRewriter() {
        protocolManager.addPacketListener(new PacketAdapter(main, ListenerPriority.HIGH, PacketType.Play.Server.SET_SLOT, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                LockedSlotsMenu menu = activeMenus.get(event.getPlayer().getUniqueId());
                if(menu == null || disabled) {
                    return;
                }

                try {
                    if(event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                        rewriteSetSlot(event.getPacket(), menu);
                    } else if(event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                        rewriteWindowItems(event.getPacket(), menu);
                    }
                } catch (RuntimeException exception) {
                    disabled = true;
                    main.getLogger().warning("Could not rewrite ignored slot preview packet. Disabling preview overlay: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
                }
            }
        });
    }

    private void rewriteSetSlot(PacketContainer packet, LockedSlotsMenu menu) {
        int windowId = packet.getIntegers().read(0);
        if(!isPreviewWindow(menu.getPlayer(), windowId)) {
            return;
        }

        int protocolSlot = packet.getIntegers().read(2);
        int playerSlot = playerSlotFromProtocolSlot(protocolSlot);
        if(playerSlot == -1) {
            return;
        }

        packet.getItemModifier().write(0, menu.createPlayerInventoryPreview(playerSlot));
    }

    private void rewriteWindowItems(PacketContainer packet, LockedSlotsMenu menu) {
        int windowId = packet.getIntegers().read(0);
        if(!isPreviewWindow(menu.getPlayer(), windowId) || packet.getItemListModifier().size() == 0) {
            return;
        }

        List<ItemStack> original = packet.getItemListModifier().read(0);
        if(original == null || original.isEmpty()) {
            return;
        }

        List<ItemStack> preview = new ArrayList<>(original);
        for(int playerSlot = 9; playerSlot <= 35; playerSlot++) {
            replaceProtocolSlot(preview, playerSlot, menu.createPlayerInventoryPreview(playerSlot));
        }
        for(int playerSlot = 0; playerSlot <= 8; playerSlot++) {
            replaceProtocolSlot(preview, playerSlot, menu.createPlayerInventoryPreview(playerSlot));
        }
        packet.getItemListModifier().write(0, preview);
    }

    private void replaceProtocolSlot(List<ItemStack> items, int playerSlot, ItemStack itemStack) {
        int protocolSlot = protocolSlotFromPlayerSlot(playerSlot);
        if(protocolSlot >= 0 && protocolSlot < items.size()) {
            items.set(protocolSlot, itemStack);
        }
    }

    private boolean isPreviewWindow(Player player, int windowId) {
        int activeWindowId = openWindowIds.getOrDefault(player.getUniqueId(), FALLBACK_PLAYER_INVENTORY_WINDOW_ID);
        return windowId == activeWindowId || windowId == FALLBACK_PLAYER_INVENTORY_WINDOW_ID;
    }

    private void sendSlot(Player player, int playerSlot, ItemStack itemStack) {
        if(disabled) {
            return;
        }

        try {
            int protocolSlot = protocolSlotFromPlayerSlot(playerSlot);
            if(protocolSlot == -1) {
                return;
            }

            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_SLOT);
            packet.getIntegers()
                    .write(0, openWindowIds.getOrDefault(player.getUniqueId(), FALLBACK_PLAYER_INVENTORY_WINDOW_ID))
                    .write(1, 0)
                    .write(2, protocolSlot);
            packet.getItemModifier().write(0, itemStack);
            protocolManager.sendServerPacket(player, packet);
        } catch (RuntimeException exception) {
            disabled = true;
            main.getLogger().warning("Could not send ignored slot preview packet. Disabling preview overlay: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private int protocolSlotFromPlayerSlot(int playerSlot) {
        if(playerSlot >= 0 && playerSlot <= 8) {
            return playerSlot + 36;
        }
        if(playerSlot >= 9 && playerSlot <= 35) {
            return playerSlot;
        }
        return -1;
    }

    private int playerSlotFromProtocolSlot(int protocolSlot) {
        if(protocolSlot >= 36 && protocolSlot <= 44) {
            return protocolSlot - 36;
        }
        if(protocolSlot >= 9 && protocolSlot <= 35) {
            return protocolSlot;
        }
        return -1;
    }
}
