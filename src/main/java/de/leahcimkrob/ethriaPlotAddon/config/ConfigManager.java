package de.leahcimkrob.ethriaPlotAddon.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Set;


public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public void reloadConfig() {
        loadConfig();
    }

    // Sprache
    public String getLanguage() {
        return config.getString("language", "de-de");
    }

    // Command-Aliases (neue Struktur)
    // Gibt den Ziel-Subcommand zurück, auf den ein Alias verweist
    public String getAliasTarget(String alias) {
        return config.getString("command_aliases." + alias);
    }

    // Prüft ob ein Alias existiert
    public boolean hasAlias(String alias) {
        return config.contains("command_aliases." + alias);
    }

    // Gibt alle konfigurierten Aliases zurück
    public Set<String> getAllAliases() {
        if (config.getConfigurationSection("command_aliases") != null) {
            return config.getConfigurationSection("command_aliases").getKeys(false);
        }
        return Set.of();
    }

    // Einstellungen
    public boolean shouldCountInvisible() {
        return config.getBoolean("settings.count_invisible", true);
    }

    public boolean shouldCountFixed() {
        return config.getBoolean("settings.count_fixed", true);
    }

    public boolean shouldIncludeMergedPlots() {
        return config.getBoolean("settings.include_merged_plots", true);
    }

    public int getMaxCountLimit() {
        return config.getInt("settings.max_count_limit", 1000);
    }

    public boolean shouldCountDroppedItems() {
        return config.getBoolean("settings.count_dropped_items", false);
    }

    public boolean isDebugBoundaries() {
        return config.getBoolean("settings.debug_boundaries", false);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug_enabled", false);
    }

    // ActionBar Live-Update Einstellungen
    public int getActionBarUpdateInterval() {
        return config.getInt("settings.actionbar_update_interval", 5);
    }

    public boolean shouldActionBarAutoHide() {
        return config.getBoolean("settings.actionbar_auto_hide", true);
    }
}
