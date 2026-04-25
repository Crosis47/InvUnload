package de.jeff_media.InvUnload;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class MinepacksHook {
	
	static boolean isMinepacksBackpack(ItemStack item) {
	    Plugin bukkitPlugin = Bukkit.getPluginManager().getPlugin("Minepacks");
	    if(bukkitPlugin == null) {
	        return false;
	    }
	    try {
	    	return (boolean) bukkitPlugin.getClass().getMethod("isBackpackItem", ItemStack.class).invoke(bukkitPlugin, item);
	    } catch (ReflectiveOperationException ignored) {
	        return false;
	    }
	}

}
