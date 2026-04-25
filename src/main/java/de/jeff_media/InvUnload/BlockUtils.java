package de.jeff_media.InvUnload;

import de.jeff_media.InvUnload.Hooks.ItemsAdderWrapper;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class BlockUtils {

	private static final EnumSet<Material> CONTAINER_TYPES;
	private static final List<String> CONTAINER_NAMES = Arrays.asList("(.*)BARREL$", "(.*)CHEST$", "^SHULKER_BOX$", "^(.*)_SHULKER_BOX$");

	static {
		CONTAINER_TYPES = EnumSet.noneOf(Material.class);
		for(Material material : Material.values()) {
			for(String regex : CONTAINER_NAMES) {
				if(material.name().matches(regex)) {
					CONTAINER_TYPES.add(material);
					break;
				}
			}
		}
	}
	
	final Main main;
	
	BlockUtils(Main main) {
		this.main=main;
	}
	
	static List<Block> findBlocksInRadius(Location loc, int radius) {
		BoundingBox box = BoundingBox.of(loc,radius,radius,radius);
		List<Chunk> chunks = getLoadedChunks(loc.getWorld(), box);
		List<Block> blocks = new ArrayList<>();
		for(Chunk chunk : chunks) {
			for(BlockState state : chunk.getTileEntities()) {
				if(state instanceof Container && isChestLikeBlock(state.getType())) {
					if(state.getLocation().distanceSquared(loc) <= radius*radius) {

						// Only chests that can be opened
						if(Main.getInstance().getConfig().getBoolean("ignore-blocked-chests",false)) {
							Block above = state.getBlock().getRelative(BlockFace.UP);
							if(state.getType() == Material.CHEST && above.getType().isSolid() && above.getType().isOccluding()) {
								continue;
							}
						}

						blocks.add(state.getBlock());
					}
				}
			}
		}
		return blocks;
	}

	private static List<Chunk> getLoadedChunks(World world, BoundingBox box) {
		List<Chunk> chunks = new ArrayList<>();
		int minChunkX = ((int) Math.floor(box.getMinX())) >> 4;
		int maxChunkX = ((int) Math.floor(box.getMaxX())) >> 4;
		int minChunkZ = ((int) Math.floor(box.getMinZ())) >> 4;
		int maxChunkZ = ((int) Math.floor(box.getMaxZ())) >> 4;
		for(int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for(int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if(world.isChunkLoaded(chunkX, chunkZ)) {
					chunks.add(world.getChunkAt(chunkX, chunkZ));
				}
			}
		}
		return chunks;
	}
	
	static List<Block> findChestsInRadius(Location loc, int radius) {
		// Todo
		return findBlocksInRadius(loc, radius);
	}

	public static boolean isChestLikeBlock(Material material) {
		return CONTAINER_TYPES.contains(material);
	}

	static boolean doesChestContain(Inventory inv, ItemStack item) {
		Main main = Main.getInstance();
		ItemsAdderWrapper itemsAdder = main.getItemsAdderWrapper();
		boolean useItemsAdder = main.getConfig().getBoolean("use-itemsadder");
		for (ItemStack otherItem : inv.getContents()) {
			if (otherItem == null) continue;
			if (otherItem.getType() == item.getType()) {

				if(!main.getEnchantmentUtils().hasMatchingEnchantments(item,otherItem)) continue;

				if (!useItemsAdder) return true;

				// Item ist NOT ItemsAdder item
				if (!itemsAdder.isItemsAdderItem(item)) {

					// Only return true if otherItem also is NOT ItemsAdder item
					if (itemsAdder.isItemsAdderItem(otherItem)) {
						continue;
					} else {
						return true;
					}
				}

				// Item IS ItemsAdder item
				else {
					// But other Item is not
					if (!itemsAdder.isItemsAdderItem(otherItem)) {
						continue;
					}
					// Both are ItemsAdder items
					else {
						if (itemsAdder.getItemsAdderName(item).equals(itemsAdder.getItemsAdderName(otherItem))) {
							return true;
						} else {
							continue;
						}
					}
				}
			}
		}
		return false;
	}

	static void sortBlockListByDistance(List<Block> blocks, Location loc) {
		blocks.sort((b1, b2)->{
			if (b1.getLocation().distance(loc) > b2.getLocation().distance(loc)) {
				return 1;
			}
			return -1;
		});
	}
	
	static Location getCenterOfBlock(Block block) {
		Location loc = block.getLocation();
		if(block.getState() instanceof Chest
				&& ((Chest)block.getState()).getInventory().getHolder() instanceof DoubleChest) {
			DoubleChest doubleChest = (DoubleChest) ((Chest)block.getState()).getInventory().getHolder();
			DoubleChestInventory doubleChestInv = (DoubleChestInventory) doubleChest.getInventory();
			loc = doubleChestInv.getLeftSide().getLocation().add(doubleChestInv.getRightSide().getLocation()).multiply(0.5);
		}
		loc.add(new Vector(0.5,1,0.5));
		return loc;
	}

	static int doesChestContainCount(Inventory inv, Material mat) {
		int count = 0;
		for(ItemStack item : inv.getContents()) {
			if(item==null) continue;
			if(item.getType() == mat) {
				count += item.getAmount();
			}
		}
		return count;
	}
}
