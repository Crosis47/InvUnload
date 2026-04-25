package de.jeff_media.InvUnload.Hooks;

import de.jeff_media.InvUnload.Main;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public final class ItemsAdderHandler extends ItemsAdderWrapper {

    private final boolean itemsAdderInstalled;
    private final Method byItemStackMethod;
    private final Method getDisplayNameMethod;

    public ItemsAdderHandler(final Main main) {
        Method resolvedByItemStackMethod = null;
        Method resolvedGetDisplayNameMethod = null;
        boolean resolvedItemsAdderInstalled = false;

        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            itemsAdderInstalled = false;
            byItemStackMethod = null;
            getDisplayNameMethod = null;
            return;
        }

        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            resolvedByItemStackMethod = customStackClass.getMethod("byItemStack", ItemStack.class);
            resolvedGetDisplayNameMethod = customStackClass.getMethod("getDisplayName");
            resolvedItemsAdderInstalled = true;
        } catch (Throwable t) {
            main.getLogger().warning("Found ItemsAdder plugin but could not hook into it.");
            t.printStackTrace();
        }

        itemsAdderInstalled = resolvedItemsAdderInstalled;
        byItemStackMethod = resolvedByItemStackMethod;
        getDisplayNameMethod = resolvedGetDisplayNameMethod;
    }

    private Object getCustomStack(ItemStack item) {
        if(!itemsAdderInstalled) return null;
        try {
            return byItemStackMethod.invoke(null, item);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Override
    public String getItemsAdderName(ItemStack item) {
        Object customStack = getCustomStack(item);
        if(customStack == null) return null;
        try {
            return (String) getDisplayNameMethod.invoke(customStack);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Override
    public boolean isItemsAdderItem(ItemStack item) {
        return getCustomStack(item) != null;
    }

}
