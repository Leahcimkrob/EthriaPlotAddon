package de.leahcimkrob.ethriaPlotAddon.commands;

import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manager für alle Subcommands
 */
public class SubcommandManager {

    private final EthriaPlotAddon plugin;
    private final Map<String, SubcommandExecutor> subcommands;

    public SubcommandManager(EthriaPlotAddon plugin) {
        this.plugin = plugin;
        this.subcommands = new HashMap<>();

        registerSubcommands();
    }

    /**
     * Registriert alle verfügbaren Subcommands
     */
    private void registerSubcommands() {
        // Count-Command
        registerSubcommand("count", new CountCommand(plugin));

        // PlotCheck-Command (mit on/off Parametern)
        registerSubcommand("check", new PlotCheckCommand(plugin));
        registerSubcommand("plotcheck", new PlotCheckCommand(plugin)); // Alias


        // Reload-Command
        registerSubcommand("reload", new ReloadCommand(plugin));

        // Help-Command (wird nach der Initialisierung registriert)
        // Wird in setHelpCommand() gesetzt
    }

    /**
     * Registriert ein Subcommand
     */
    public void registerSubcommand(String name, SubcommandExecutor executor) {
        subcommands.put(name.toLowerCase(), executor);
    }

    /**
     * Führt ein Subcommand aus
     */
    public boolean executeSubcommand(String name, Player player, String[] args) {
        SubcommandExecutor executor = subcommands.get(name.toLowerCase());
        if (executor == null) {
            return false;
        }

        // Prüfe Berechtigung
        if (!executor.hasPermission(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return true;
        }

        // Führe Subcommand aus
        return executor.execute(player, args);
    }

    /**
     * Prüft ob ein Subcommand existiert
     */
    public boolean hasSubcommand(String name) {
        return subcommands.containsKey(name.toLowerCase());
    }

    /**
     * Prüft ob ein Spieler Berechtigung für ein Subcommand hat
     */
    public boolean hasPermissionForSubcommand(String name, Player player) {
        SubcommandExecutor executor = subcommands.get(name.toLowerCase());
        return executor != null && executor.hasPermission(player);
    }

    /**
     * Gibt alle verfügbaren Subcommand-Namen zurück
     */
    public Set<String> getSubcommandNames() {
        return subcommands.keySet();
    }

    /**
     * Gibt einen Subcommand-Executor zurück
     */
    public SubcommandExecutor getSubcommand(String name) {
        return subcommands.get(name.toLowerCase());
    }

    /**
     * Gibt alle Subcommands zurück, für die der Spieler berechtigt ist
     */
    public Map<String, SubcommandExecutor> getAvailableSubcommands(Player player) {
        Map<String, SubcommandExecutor> available = new HashMap<>();
        for (Map.Entry<String, SubcommandExecutor> entry : subcommands.entrySet()) {
            if (entry.getValue().hasPermission(player)) {
                available.put(entry.getKey(), entry.getValue());
            }
        }
        return available;
    }

    /**
     * Setzt das Help-Command nach der Initialisierung
     * (um Circular Dependency zu vermeiden)
     */
    public void setHelpCommand() {
        registerSubcommand("help", new HelpCommand(plugin, this));
        registerSubcommand("?", new HelpCommand(plugin, this)); // Alias
    }
}
