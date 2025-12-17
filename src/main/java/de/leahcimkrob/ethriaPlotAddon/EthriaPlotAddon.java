package de.leahcimkrob.ethriaPlotAddon;

import com.plotsquared.core.plot.Plot;
import de.leahcimkrob.ethriaPlotAddon.commands.SubcommandManager;
import de.leahcimkrob.ethriaPlotAddon.config.ConfigManager;
import de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration;
import de.leahcimkrob.ethriaPlotAddon.lang.MessageManager;
import de.leahcimkrob.ethriaPlotAddon.util.DebugLogger;
import de.leahcimkrob.ethriaPlotAddon.util.EntityCounter;
import de.leahcimkrob.ethriaPlotAddon.util.EntityGroupManager;
import de.leahcimkrob.ethriaPlotAddon.util.PermissionManager;
import de.leahcimkrob.ethriaPlotAddon.util.PlotCountTabCompleter;
import de.leahcimkrob.ethriaPlotAddon.util.TitleUpdateManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EthriaPlotAddon extends JavaPlugin {
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DebugLogger debugLogger;
    private SubcommandManager subcommandManager;
    private TitleUpdateManager titleUpdateManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("EthriaPlotAddon wird gestartet...");

        // Initialisiere Config Manager
        configManager = new ConfigManager(this);

        // Initialisiere Message Manager mit der konfigurierten Sprache
        messageManager = new MessageManager(this, configManager.getLanguage());

        // Initialisiere Debug Logger
        debugLogger = new DebugLogger(this);

        // Initialisiere Subcommand Manager
        subcommandManager = new SubcommandManager(this);

        // Initialisiere Title Update Manager
        titleUpdateManager = new TitleUpdateManager(this);

        // Setze Help-Command nach Initialisierung
        subcommandManager.setHelpCommand();

        // Aktiviere Debug-Modus basierend auf Config
        debugLogger.setEnabled(configManager.isDebugEnabled());

        // Lösche alte debug.log bei Plugin-Start
        if (configManager.isDebugEnabled()) {
            debugLogger.clearLog();
            debugLogger.debug("=== EthriaPlotAddon Debug-Session gestartet ===");
            debugLogger.debug("Debug-Modus aktiviert, alle Debug-Meldungen werden in debug.log geschrieben");
        }

        // Check if PlotSquared is available
        if (getServer().getPluginManager().getPlugin("PlotSquared") == null) {
            getLogger().severe(messageManager.getMessage("plotsquared_not_found"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registriere Command-Aliases aus der Config
        registerCommandAliases();

        // Registriere Tab-Completer für Hauptbefehl
        Objects.requireNonNull(this.getCommand("plotaddon")).setTabCompleter(new PlotCountTabCompleter(messageManager, configManager));

        getLogger().info(messageManager.getMessage("plotsquared_found"));
        getLogger().info(messageManager.getMessage("plugin_enabled"));
    }

    @Override
    public void onDisable() {
        // Cleanup Title Update Manager
        if (titleUpdateManager != null) {
            titleUpdateManager.shutdown();
        }

        // Plugin shutdown logic
        getLogger().info(messageManager.getMessage("plugin_disabled"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Accept main command, plugin.yml alias, and dynamic aliases
        String commandName = command.getName().toLowerCase();

        if (!commandName.equals("plotaddon") && !commandName.equals("plotcount") &&
            !configManager.hasAlias(commandName)) {
            return false;
        }

        // Check if sender is player
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;

        // Handle built-in alias usage: /plotcount <entity> becomes /plotaddon count <entity>
        if (commandName.equals("plotcount")) {
            return subcommandManager.executeSubcommand("count", player, args);
        }

        // Handle dynamic aliases from config
        if (configManager.hasAlias(commandName)) {
            String targetSubcommand = configManager.getAliasTarget(commandName);
            if (targetSubcommand != null) {
                return subcommandManager.executeSubcommand(targetSubcommand, player, args);
            }
        }

        // Handle main command: /plotaddon <subcommand>
        if (args.length == 0) {
            // Zeige Hilfe wenn keine Argumente
            return subcommandManager.executeSubcommand("help", player, new String[0]);
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // Delegiere an SubcommandManager
        if (subcommandManager.hasSubcommand(subCommand)) {
            return subcommandManager.executeSubcommand(subCommand, player, subArgs);
        } else {
            // Unbekannter Subcommand - zeige Hilfe
            return subcommandManager.executeSubcommand("help", player, new String[0]);
        }
    }

    /**
     * Lädt die Konfiguration neu (für ReloadCommand)
     */
    public void reloadConfigs() {
        configManager.reloadConfig();
        messageManager.setLanguage(configManager.getLanguage());

        // Re-register aliases after config reload
        registerCommandAliases();
    }

    private void registerCommandAliases() {
        try {
            Set<String> aliases = configManager.getAllAliases();
            if (!aliases.isEmpty()) {
                for (String alias : aliases) {
                    String targetSubcommand = configManager.getAliasTarget(alias);
                    if (targetSubcommand != null && !targetSubcommand.trim().isEmpty()) {
                        registerDynamicCommand(alias.trim(), targetSubcommand.trim());
                    }
                }
                getLogger().info("Registrierte " + aliases.size() + " Command-Aliases: " + String.join(", ", aliases));
            }
        } catch (Exception e) {
            getLogger().warning("Fehler beim Registrieren der Command-Aliases: " + e.getMessage());
        }
    }

    private void registerDynamicCommand(String alias, String targetSubcommand) {
        try {
            // Create a command that delegates to plotaddon with the specific subcommand
            org.bukkit.command.Command aliasCommand = new org.bukkit.command.Command(alias) {
                @Override
                public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                    // Redirect to plotaddon with the target subcommand
                    String[] newArgs = new String[args.length + 1];
                    newArgs[0] = targetSubcommand;
                    System.arraycopy(args, 0, newArgs, 1, args.length);

                    return onCommand(sender, getCommand("plotaddon"), "plotaddon", newArgs);
                }

                @Override
                public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                    // Delegate to our TabCompleter with modified args
                    String[] newArgs = new String[args.length + 1];
                    newArgs[0] = targetSubcommand;
                    System.arraycopy(args, 0, newArgs, 1, args.length);

                    PlotCountTabCompleter tabCompleter = new PlotCountTabCompleter(messageManager, configManager);
                    return tabCompleter.onTabComplete(sender, getCommand("plotaddon"), "plotaddon", newArgs);
                }
            };

            // Set properties
            aliasCommand.setDescription("Alias für /plotaddon " + targetSubcommand);
            aliasCommand.setUsage("/" + alias + " [args...]");
            // Keine Standard-Permission - wird durch Subcommand geprüft

            // Register the command
            getServer().getCommandMap().register("ethriaplotaddon", aliasCommand);

        } catch (Exception e) {
            getLogger().warning("Fehler beim Registrieren des Alias '" + alias + "': " + e.getMessage());
        }
    }

    // Getter für andere Klassen
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Getter für DebugLogger
     */
    public DebugLogger getDebugLogger() {
        return debugLogger;
    }

    /**
     * Getter für TitleUpdateManager
     */
    public TitleUpdateManager getTitleUpdateManager() {
        return titleUpdateManager;
    }
}
