package de.jeff_media.InvUnload.Hooks;

import de.jeff_media.InvUnload.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class CoreProtectHook {

    final Main main;
    Method getApiMethod;
    Method logContainerTransactionMethod;
    boolean initialized = false;
    boolean disabled = false;

    public CoreProtectHook(Main main) {
        this.main = main;
    }

    public void logCoreProtect(String user, Inventory destination) {

        if(disabled) return;

        if(!main.getConfig().getBoolean("use-coreprotect")) return;

        Plugin coreProtectPlugin = main.getServer().getPluginManager().getPlugin("CoreProtect");
        if(coreProtectPlugin == null) {
            disabled=true;
            return;
        }

        Location location = destination.getLocation();
        if(location == null) {
            return;
        }

        try {
            if(!initialized) {
                getApiMethod = coreProtectPlugin.getClass().getMethod("getAPI");
                Object api = getApiMethod.invoke(coreProtectPlugin);
                if(api == null) {
                    disabled = true;
                    return;
                }
                logContainerTransactionMethod = api.getClass().getMethod("logContainerTransaction", String.class, Location.class);
                initialized = true;
            }

            Object api = getApiMethod.invoke(coreProtectPlugin);
            if(api == null) {
                disabled = true;
                return;
            }
            logContainerTransactionMethod.invoke(api, user, location);
        } catch (ReflectiveOperationException e) {
            main.getLogger().warning("Could not log to CoreProtect because your version of CoreProtect is too old or incompatible.");
            disabled = true;
        }
    }

}
