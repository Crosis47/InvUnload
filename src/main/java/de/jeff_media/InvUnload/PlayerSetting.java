package de.jeff_media.InvUnload;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PlayerSetting {

    final BlackList blacklist;
    boolean unloadHotbar;
    boolean dumpHotbar;
    final Set<Integer> lockedSlots;

    PlayerSetting() {
        blacklist = new BlackList();
        lockedSlots = new HashSet<>();
    }

    BlackList getBlacklist() {
        return blacklist;
    }

    PlayerSetting(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        blacklist = new BlackList(yaml.getStringList("blacklist"));
        unloadHotbar = yaml.getBoolean("unloadHotbar",false);
        dumpHotbar = yaml.getBoolean("dumpHotbar",false);
        lockedSlots = new HashSet<>(yaml.getIntegerList("lockedSlots"));
    }

    boolean isSlotLocked(int slot) {
        return lockedSlots.contains(slot);
    }

    void toggleLockedSlot(int slot) {
        if(!isValidSlot(slot)) return;
        if(!lockedSlots.add(slot)) {
            lockedSlots.remove(slot);
        }
    }

    void setSlotLocked(int slot, boolean locked) {
        if(!isValidSlot(slot)) return;
        if(locked) {
            lockedSlots.add(slot);
        } else {
            lockedSlots.remove(slot);
        }
    }

    void setSlotsLocked(Collection<Integer> slots, boolean locked) {
        for(int slot : slots) {
            setSlotLocked(slot, locked);
        }
    }

    void clearLockedSlots() {
        lockedSlots.clear();
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot <= 35;
    }

    void save(File file,Main main) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("blacklist",blacklist.toStringList());
        yaml.set("unloadHotbar",unloadHotbar);
        yaml.set("dumpHotbar",dumpHotbar);
        yaml.set("lockedSlots",lockedSlots.stream().sorted().toList());
        try {
            yaml.save(file);
        } catch (IOException e) {
            main.getLogger().warning("Could not save playerdata file "+file.getPath());
            //e.printStackTrace();
        }
    }

}
