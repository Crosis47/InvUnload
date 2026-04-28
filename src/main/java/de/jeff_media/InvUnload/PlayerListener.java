package de.jeff_media.InvUnload;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class PlayerListener implements Listener {

    final Main main;

    PlayerListener(Main main) {
        this.main=main;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof LockedSlotsMenu menu)) {
            return;
        }

        event.setCancelled(true);
        if(event.getRawSlot() < 0) {
            return;
        }

        if(event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            menu.handleMenuClick(event.getRawSlot());
            return;
        }

        if(event.getRawSlot() < event.getView().countSlots()) {
            menu.handlePlayerInventoryClick(event.getView().convertSlot(event.getRawSlot()));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if(event.getView().getTopInventory().getHolder() instanceof LockedSlotsMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if(event.getView().getTopInventory().getHolder() instanceof LockedSlotsMenu menu) {
            menu.close();
        }
    }

}
