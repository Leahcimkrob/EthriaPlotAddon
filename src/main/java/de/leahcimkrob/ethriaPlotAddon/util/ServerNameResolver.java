package de.leahcimkrob.ethriaPlotAddon.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.WeakHashMap;

/** Resolves one stable database server identifier for Plot Info and Plot Check. */
public final class ServerNameResolver {
    private static final String LUCKPERMS_API = "net.luckperms.api.LuckPerms";
    private static final Map<JavaPlugin, String> RESOLVED_NAMES = new WeakHashMap<>();

    private ServerNameResolver() { }

    public static synchronized String resolve(JavaPlugin plugin) {
        String cached = RESOLVED_NAMES.get(plugin);
        if (cached != null) return cached;

        String resolved = resolveAutomatically(plugin);
        RESOLVED_NAMES.put(plugin, resolved);
        return resolved;
    }

    private static String resolveAutomatically(JavaPlugin plugin) {
        Plugin luckPermsPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (luckPermsPlugin != null && luckPermsPlugin.isEnabled()) {
            try {
                Class<?> apiClass = luckPermsPlugin.getClass().getClassLoader().loadClass(LUCKPERMS_API);
                Object luckPerms = Bukkit.getServicesManager().load(apiClass);
                if (luckPerms != null) {
                    Object result = apiClass.getMethod("getServerName").invoke(luckPerms);
                    if (result instanceof String name && !name.isBlank()) {
                        String resolved = name.trim();
                        if ("global".equalsIgnoreCase(resolved)
                                && Bukkit.getServer().getServerConfig().isProxyEnabled()) {
                            plugin.getLogger().warning("LuckPerms verwendet hinter einem Proxy den Servernamen 'global'; "
                                    + "für eine eindeutige Datenbankkennung wird Paper-Name:Port verwendet.");
                        } else {
                            plugin.getLogger().info("Verwende LuckPerms-Serverkennung für Datenbankeinträge: " + resolved);
                            return resolved;
                        }
                    }
                }
            } catch (ReflectiveOperationException | LinkageError exception) {
                plugin.getLogger().warning("LuckPerms-Serverkennung konnte nicht gelesen werden: " + exception.getMessage());
            }
        }

        String fallback = Bukkit.getServer().getName() + ":" + Bukkit.getServer().getPort();
        plugin.getLogger().info("Verwende Paper-Serverkennung als Datenbank-Fallback: " + fallback);
        return fallback;
    }
}
