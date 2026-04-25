package de.jeff_media.InvUnload.Hooks;

import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import de.jeff_media.InvUnload.BlockUtils;
import de.jeff_media.InvUnload.Main;

import java.lang.reflect.Method;

public class ChestSortHook {
	
	final Main main;
	private Method hasSortingEnabledMethod;
	private Method sortInventoryMethod;
	private boolean available;
	
	public ChestSortHook(Main main) {
		this.main=main;
		try {
			Class<?> chestSortApiClass = Class.forName("de.jeff_media.chestsort.api.ChestSortAPI");
			hasSortingEnabledMethod = chestSortApiClass.getMethod("hasSortingEnabled", Player.class);
			sortInventoryMethod = chestSortApiClass.getMethod("sortInventory", Inventory.class);
			available = true;
		} catch (ReflectiveOperationException ignored) {
			available = false;
		}
	}
	
	public boolean shouldSort(Player p) {
		if(!main.useChestSort || !available) return false;
		if(main.getConfig().getBoolean("force-chestsort")) return true;
		try {
			return (boolean) hasSortingEnabledMethod.invoke(null, p);
		} catch (ReflectiveOperationException e) {
			main.getLogger().warning("Could not query ChestSort. Disabling ChestSort integration.");
			main.useChestSort = false;
			return false;
		}
	}
	
	public void sort(Block block) {
		if(!main.useChestSort || !available) return;
		if(!BlockUtils.isChestLikeBlock(block.getType())) return;
		Inventory inv = ((Container) block.getState()).getInventory();
		try {
			sortInventoryMethod.invoke(null, inv);
		} catch (ReflectiveOperationException e) {
			main.getLogger().warning("Could not sort inventory via ChestSort. Disabling ChestSort integration.");
			main.useChestSort = false;
		}
	}

}
