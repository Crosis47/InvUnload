package de.jeff_media.InvUnload.Hooks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import de.jeff_media.InvUnload.Main;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

public class PlotSquared5Hook implements PlotSquaredUniversalHook {

	private final Method getLocationMethod;
	private final Method getPlotAbsMethod;
	private final Method getTrustedMethod;
	private final Method isOwnerMethod;

	public PlotSquared5Hook() {
		try {
			Class<?> bukkitUtilClass = Class.forName("com.plotsquared.bukkit.util.BukkitUtil");
			getLocationMethod = bukkitUtilClass.getMethod("getLocation", Location.class);
			getPlotAbsMethod = getLocationMethod.getReturnType().getMethod("getPlotAbs");
			Class<?> plotClass = Class.forName("com.plotsquared.core.plot.Plot");
			getTrustedMethod = plotClass.getMethod("getTrusted");
			isOwnerMethod = plotClass.getMethod("isOwner", UUID.class);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not initialize PlotSquared v5 hook", e);
		}
	}

	@Override
	public boolean isBlockedByPlotSquared(Block block, Player player, Main main) {
		try {
			Object location = getLocationMethod.invoke(null, block.getLocation());
			Object plot = getPlotAbsMethod.invoke(location);

			if(plot == null) return !main.getConfig().getBoolean("plotsquared-allow-outside-plots");

			Collection<UUID> trusted = (Collection<UUID>) getTrustedMethod.invoke(plot);
			if(trusted.contains(player.getUniqueId())
					&& main.getConfig().getBoolean("plotsquared-allow-when-trusted")) return false;

			return !(boolean) isOwnerMethod.invoke(plot, player.getUniqueId());
		} catch (ReflectiveOperationException e) {
			main.getLogger().warning("Could not query PlotSquared v5 hook.");
			return false;
		}
	}
	
	

}
