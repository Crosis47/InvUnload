package de.jeff_media.InvUnload;

import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Locale;

public class EnumUtils {
	
	static boolean soundExists(String value) {
		if(value == null) return false;
		try {
			Sound.valueOf(value.trim().toUpperCase(Locale.ROOT));
			return true;
		} catch (IllegalArgumentException ignored) {
			return false;
		}
	}
	
	static boolean particleExists(String value) {
		if(value == null) return false;
		try {
			Particle.valueOf(value.trim().toUpperCase(Locale.ROOT));
			return true;
		} catch (IllegalArgumentException ignored) {
			return false;
		}
	}

}
