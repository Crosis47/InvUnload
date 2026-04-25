package de.jeff_media.InvUnload;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        if(event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        menu.handleClick(event.getRawSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if(event.getView().getTopInventory().getHolder() instanceof LockedSlotsMenu) {
            event.setCancelled(true);
        }
    }

}
