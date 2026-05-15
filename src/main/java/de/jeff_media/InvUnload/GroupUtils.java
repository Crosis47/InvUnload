package de.jeff_media.InvUnload;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class GroupUtils {
	
	final Main main;
	YamlConfiguration yaml;
	LinkedHashMap<String,Group> groups;
	GroupUtils(Main main,File yamlFile) {
		this.main=main;
		if(!yamlFile.exists()) {
			main.getLogger().info("groups.yml does not exist, skipping custom group settings.");
			return;
		}
		this.yaml=YamlConfiguration.loadConfiguration(yamlFile);
		groups = new LinkedHashMap<>();
		
		for(String groupName : yaml.getKeys(false)) {
			int defaultRadius = yaml.getInt(groupName+".default-chest-radius",-1);
			int maxRadius = yaml.getInt(groupName+".max-chest-radius",-1);
			int defaultSearchRadius = yaml.getInt(groupName+".default-search-radius",defaultRadius);
			int maxSearchRadius = yaml.getInt(groupName+".max-search-radius",maxRadius);
			int searchTakeRadius = yaml.contains(groupName+".search-take-radius") ? yaml.getInt(groupName+".search-take-radius") : Integer.MIN_VALUE;
			groups.put(groupName, new Group(defaultRadius, maxRadius, defaultSearchRadius, maxSearchRadius, searchTakeRadius));
		}
	}
	
	int getDefaultRadiusPerPlayer(Player p) {
		if(yaml==null) return main.getConfig().getInt("default-chest-radius");
		Iterator<String> it = groups.keySet().iterator();
		int bestValueFound = -1;
		while(it.hasNext()) {
			String group = it.next();
			if(!p.hasPermission("invunload.group."+group)) continue;
			int defaultRadius = groups.get(group).defaultRadius;
			bestValueFound = (defaultRadius>bestValueFound) ? defaultRadius : bestValueFound;
		}
		return bestValueFound == -1 ? main.getConfig().getInt("default-chest-radius") : bestValueFound;
	}
	
	int getMaxRadiusPerPlayer(Player p) {
		if(yaml==null) return main.getConfig().getInt("max-chest-radius");
		Iterator<String> it = groups.keySet().iterator();
		int bestValueFound = -1;
		while(it.hasNext()) {
			String group = it.next();
			if(!p.hasPermission("invunload.group."+group)) continue;
			int maxRadius = groups.get(group).maxRadius;
			bestValueFound = (maxRadius>bestValueFound) ? maxRadius : bestValueFound;
		}
		return bestValueFound == -1 ? main.getConfig().getInt("max-chest-radius") : bestValueFound;
	}

	int getDefaultSearchRadiusPerPlayer(Player p) {
		if(yaml==null) return main.getConfig().getInt("default-search-radius", 10);
		Iterator<String> it = groups.keySet().iterator();
		int bestValueFound = -1;
		while(it.hasNext()) {
			String group = it.next();
			if(!p.hasPermission("invunload.group."+group)) continue;
			int defaultSearchRadius = groups.get(group).defaultSearchRadius;
			bestValueFound = (defaultSearchRadius>bestValueFound) ? defaultSearchRadius : bestValueFound;
		}
		return bestValueFound == -1 ? main.getConfig().getInt("default-search-radius", 10) : bestValueFound;
	}

	int getMaxSearchRadiusPerPlayer(Player p) {
		if(yaml==null) return main.getConfig().getInt("max-search-radius", 20);
		Iterator<String> it = groups.keySet().iterator();
		int bestValueFound = -1;
		while(it.hasNext()) {
			String group = it.next();
			if(!p.hasPermission("invunload.group."+group)) continue;
			int maxSearchRadius = groups.get(group).maxSearchRadius;
			bestValueFound = (maxSearchRadius>bestValueFound) ? maxSearchRadius : bestValueFound;
		}
		return bestValueFound == -1 ? main.getConfig().getInt("max-search-radius", 20) : bestValueFound;
	}

	int getSearchTakeRadiusPerPlayer(Player p) {
		if(yaml==null) return main.getConfig().getInt("search-take-radius", 10);
		Iterator<String> it = groups.keySet().iterator();
		int bestValueFound = Integer.MIN_VALUE;
		while(it.hasNext()) {
			String group = it.next();
			if(!p.hasPermission("invunload.group."+group)) continue;
			int searchTakeRadius = groups.get(group).searchTakeRadius;
			if(searchTakeRadius == Integer.MIN_VALUE) continue;
			bestValueFound = (searchTakeRadius>bestValueFound) ? searchTakeRadius : bestValueFound;
		}
		return bestValueFound == Integer.MIN_VALUE ? main.getConfig().getInt("search-take-radius", 10) : bestValueFound;
	}
	
	static class Group {
		final int defaultRadius;
		final int maxRadius;
		final int defaultSearchRadius;
		final int maxSearchRadius;
		final int searchTakeRadius;
		
		Group(int defaultRadius, int maxRadius, int defaultSearchRadius, int maxSearchRadius, int searchTakeRadius) {
			this.defaultRadius = defaultRadius;
			this.maxRadius = maxRadius;
			this.defaultSearchRadius = defaultSearchRadius;
			this.maxSearchRadius = maxSearchRadius;
			this.searchTakeRadius = searchTakeRadius;
		}
	}
}
