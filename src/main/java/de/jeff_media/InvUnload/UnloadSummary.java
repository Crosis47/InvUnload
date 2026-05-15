package de.jeff_media.InvUnload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UnloadSummary {

	private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
	
	final HashMap<Location,EnumMap<Material,Integer>> unloads;
	final HashMap<Location,EnumMap<Material,ArrayList<SearchSlot>>> searchSlots;
	
	UnloadSummary() {

		//System.out.println("Creating summary");
		unloads = new HashMap<>();
		searchSlots = new HashMap<>();
	}
	
	void protocolUnload(Location loc, Material mat, int amount) {
		if(amount==0) return;
		if(!unloads.containsKey(loc)) {
			unloads.put(loc, new EnumMap<>(Material.class));
			unloads.get(loc).put(mat, amount);
		} else {
			if(unloads.get(loc).containsKey(mat)) {
				unloads.get(loc).put(mat, unloads.get(loc).get(mat)+amount);
			} else {
				unloads.get(loc).put(mat, amount);
			}
		}
	}

	void protocolSearchResult(Location loc, Material mat, int amount, int slot, String takeCommand, ItemStack item) {
		protocolUnload(loc, mat, amount);
		if(!searchSlots.containsKey(loc)) {
			searchSlots.put(loc, new EnumMap<>(Material.class));
		}
		if(!searchSlots.get(loc).containsKey(mat)) {
			searchSlots.get(loc).put(mat, new ArrayList<>());
		}
		searchSlots.get(loc).get(mat).add(new SearchSlot(slot, takeCommand, item));
	}
	
	String loc2str(Location loc) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		String name = loc.getBlock().getType().name();
		BlockState state = loc.getWorld().getBlockAt(x,y,z).getState();
		if(state instanceof Container) {
			Container container = (Container) state;
			if(container.getCustomName() != null) {
				name = container.getCustomName();
			}
		}
		return String.format(ChatColor.LIGHT_PURPLE + "§l%s   §r§a§lX: §f%d §a§lY: §f%d §a§lZ: §f%d", name,x,y,z);
	}
	
	String amount2str(int amount) {
		return String.format(ChatColor.DARK_PURPLE+"|§7%5dx  ", amount);
	}
	
	String slots2str(Location loc, Material mat) {
		List<SearchSlot> slots = getSearchSlotEntries(loc, mat);
		if(slots.isEmpty()) return "";

		StringBuilder builder = new StringBuilder(ChatColor.GRAY.toString());
		builder.append(slots.size() == 1 ? " (slot: " : " (slots: ");
		for(int i = 0; i < slots.size(); i++) {
			if(i > 0) builder.append(", ");
			builder.append(slots.get(i).getDisplaySlot());
		}
		builder.append(")");
		return builder.toString();
	}

	List<Integer> getSearchSlots(Location loc, Material mat) {
		ArrayList<Integer> slots = new ArrayList<>();
		for(SearchSlot slot : getSearchSlotEntries(loc, mat)) {
			slots.add(slot.getDisplaySlot());
		}
		Collections.sort(slots);
		return slots;
	}

	List<SearchSlot> getSearchSlotEntries(Location loc, Material mat) {
		if(!searchSlots.containsKey(loc)) return Collections.emptyList();
		if(!searchSlots.get(loc).containsKey(mat)) return Collections.emptyList();
		ArrayList<SearchSlot> slots = new ArrayList<>(searchSlots.get(loc).get(mat));
		Collections.sort(slots);
		return slots;
	}

	void print(PrintRecipient recipient, Player p) {
		if(unloads.size()>0) printTo(recipient,p," ");
		for(Entry<Location,EnumMap<Material,Integer>> entry : unloads.entrySet()) {
			printTo(recipient,p," ");
			printTo(recipient,p,loc2str(entry.getKey()));
			EnumMap<Material,Integer> map = entry.getValue();
			for(Entry<Material,Integer> entry2 : map.entrySet()) {
				if(recipient == PrintRecipient.PLAYER && p != null && hasClickableSearchSlots(entry.getKey(), entry2.getKey())) {
					p.sendMessage(createSearchLine(entry.getKey(), entry2.getKey(), entry2.getValue()));
				} else {
					printTo(recipient,p,
							amount2str(entry2.getValue()) + ChatColor.GOLD + entry2.getKey().name() + slots2str(entry.getKey(), entry2.getKey()));
				}
			}
			//printTo(recipient,p," ");
		}
	}

	private boolean hasClickableSearchSlots(Location loc, Material mat) {
		for(SearchSlot slot : getSearchSlotEntries(loc, mat)) {
			if(slot.getTakeCommand() != null) return true;
		}
		return false;
	}

	private Component createSearchLine(Location loc, Material mat, int amount) {
		Component line = legacy(amount2str(amount) + ChatColor.GOLD + mat.name());
		List<SearchSlot> slots = getSearchSlotEntries(loc, mat);
		if(slots.isEmpty()) return line;

		line = line.append(legacy(ChatColor.GRAY + (slots.size() == 1 ? " (slot: " : " (slots: ")));
		for(int i = 0; i < slots.size(); i++) {
			if(i > 0) {
				line = line.append(legacy(ChatColor.GRAY + ", "));
			}
			line = line.append(createTakeLink(slots.get(i)));
		}
		line = line.append(legacy(ChatColor.GRAY + ")"));
		return line;
	}

	private Component createTakeLink(SearchSlot slot) {
		Component link = Component.text(slot.getDisplaySlot() + " [take]");
		if(slot.getTakeCommand() != null) {
			link = link.color(NamedTextColor.AQUA)
					.decorate(TextDecoration.UNDERLINED)
					.clickEvent(ClickEvent.runCommand(slot.getTakeCommand()));
			ItemStack itemSnapshot = slot.getItemSnapshot();
			if(itemSnapshot != null) {
				link = link.hoverEvent(itemSnapshot.asHoverEvent(showItem -> showItem));
			}
		} else {
			link = link.color(NamedTextColor.GRAY);
		}
		return link;
	}

	private Component legacy(String text) {
		return LEGACY_SERIALIZER.deserialize(text);
	}
	
	enum PrintRecipient {
		PLAYER, CONSOLE
	}
	
	void printTo(PrintRecipient recipient, Player p, String text) {
		//System.out.println("Printing");
		if(recipient == PrintRecipient.CONSOLE) {
			System.out.println(text);
		} else {
			p.sendMessage(text);
		}
	}

	static class SearchSlot implements Comparable<SearchSlot> {
		private final int slot;
		private final String takeCommand;
		private final ItemStack itemSnapshot;

		SearchSlot(int slot, String takeCommand, ItemStack item) {
			this.slot = slot;
			this.takeCommand = takeCommand;
			this.itemSnapshot = item == null ? null : item.clone();
		}

		int getDisplaySlot() {
			return slot + 1;
		}

		String getTakeCommand() {
			return takeCommand;
		}

		ItemStack getItemSnapshot() {
			return itemSnapshot == null ? null : itemSnapshot.clone();
		}

		@Override
		public int compareTo(SearchSlot other) {
			return Integer.compare(slot, other.slot);
		}
	}
}
