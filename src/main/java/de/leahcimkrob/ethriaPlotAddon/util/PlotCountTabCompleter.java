package de.leahcimkrob.ethriaPlotAddon.util;

import de.leahcimkrob.ethriaPlotAddon.config.ConfigManager;
import de.leahcimkrob.ethriaPlotAddon.lang.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PlotCountTabCompleter implements TabCompleter {

    private final MessageManager messageManager;
    private final ConfigManager configManager;

    public PlotCountTabCompleter(MessageManager messageManager, ConfigManager configManager) {
        this.messageManager = messageManager;
        this.configManager = configManager;
    }

    // Backwards compatibility constructor
    public PlotCountTabCompleter(MessageManager messageManager) {
        this.messageManager = messageManager;
        this.configManager = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        // Prüfe ob Spieler mindestens eine Berechtigung hat
        if (!hasAnyPermission(player)) {
            return new ArrayList<>();
        }

        String commandName = command.getName().toLowerCase();

        // Handle /plotcount <entity> (built-in alias usage)
        if (commandName.equals("plotcount")) {
            if (args.length == 1) {
                return getEntityCompletions(player, args[0]);
            }
            return new ArrayList<>();
        }

        // Handle dynamic aliases from config
        if (configManager != null && configManager.hasAlias(commandName)) {
            String targetSubcommand = configManager.getAliasTarget(commandName);
            if ("count".equals(targetSubcommand)) {
                if (args.length == 1) {
                    return getEntityCompletions(player, args[0]);
                }
            } else if ("plotinfo".equals(targetSubcommand)
                    && (player.hasPermission("ethriaplotaddon.plotinfo.view")
                    || player.hasPermission("ethriaplotaddon.plotinfo.edit")
                    || player.hasPermission("ethriaplotaddon.plotinfo.list")) && args.length == 1) {
                List<String> options = new ArrayList<>();
                if (player.hasPermission("ethriaplotaddon.plotinfo.edit")) {
                    options.add("add");
                }
                if (player.hasPermission("ethriaplotaddon.plotinfo.list")) options.add("list");
                return options.stream().filter(option -> option.startsWith(args[0].toLowerCase())).toList();
            } else if ("plotinfo".equals(targetSubcommand) && args.length == 2
                    && args[0].equalsIgnoreCase("list")
                    && player.hasPermission("ethriaplotaddon.plotinfo.list")) {
                return getOnlinePlayerCompletions(args[1]);
            }
            return new ArrayList<>();
        }

        // Handle /plotaddon <subcommand>
        if (commandName.equals("plotaddon")) {
            if (args.length == 1) {
                // First argument: subcommands
                List<String> subcommands = new ArrayList<>();
                if ("count".startsWith(args[0].toLowerCase())) {
                    subcommands.add("count");
                }
                if ("check".startsWith(args[0].toLowerCase()) && PermissionManager.canCheckPlots(player)) {
                    subcommands.add("check");
                }
                if ("help".startsWith(args[0].toLowerCase())) {
                    subcommands.add("help");
                }
                if (PermissionManager.canReload(player) && "reload".startsWith(args[0].toLowerCase())) {
                    subcommands.add("reload");
                }
                if (player.hasPermission("ethriaplotaddon.admin.convert")
                        && "convert".startsWith(args[0].toLowerCase())) {
                    subcommands.add("convert");
                }
                if (player.hasPermission("ethriaplotaddon.admin.databaseimport")) {
                    if ("plotinfoimport".startsWith(args[0].toLowerCase())) subcommands.add("plotinfoimport");
                    if ("plotcheckimport".startsWith(args[0].toLowerCase())) subcommands.add("plotcheckimport");
                }
                if ((player.hasPermission("ethriaplotaddon.plotinfo.view")
                        || player.hasPermission("ethriaplotaddon.plotinfo.edit")
                        || player.hasPermission("ethriaplotaddon.plotinfo.list"))
                        && "plotinfo".startsWith(args[0].toLowerCase())) {
                    subcommands.add("plotinfo");
                }
                return subcommands;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("count")) {
                // Second argument for count subcommand: entities
                return getEntityCompletions(player, args[1]);
            } else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
                // Tab-Completion für check command: on, off (nur Admins), -v (alle)
                List<String> options = new ArrayList<>();

                // on/off nur für Admins
                if (PermissionManager.hasAdminPermission(player)) {
                    if ("on".startsWith(args[1].toLowerCase())) {
                        options.add("on");
                    }
                    if ("off".startsWith(args[1].toLowerCase())) {
                        options.add("off");
                    }
                }

                // -v für alle mit check-Permission
                if ("-v".startsWith(args[1].toLowerCase())) {
                    options.add("-v");
                }

                return options;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("convert")
                    && player.hasPermission("ethriaplotaddon.admin.convert")) {
                return List.of("sqlite", "mysql").stream()
                        .filter(type -> type.startsWith(args[1].toLowerCase()))
                        .toList();
            } else if (args.length == 3 && args[0].equalsIgnoreCase("convert")
                    && player.hasPermission("ethriaplotaddon.admin.convert")) {
                String source = args[1].toLowerCase();
                return List.of("sqlite", "mysql").stream()
                        .filter(type -> !type.equals(source) && type.startsWith(args[2].toLowerCase()))
                        .toList();
            } else if ((args.length == 2 || args.length == 3)
                    && (args[0].equalsIgnoreCase("plotinfoimport") || args[0].equalsIgnoreCase("plotcheckimport"))
                    && player.hasPermission("ethriaplotaddon.admin.databaseimport")) {
                String source = args.length == 3 ? args[1].toLowerCase(Locale.ROOT) : "";
                return List.of("sqlite", "mysql").stream()
                        .filter(type -> (args.length != 3 || !type.equals(source))
                                && type.startsWith(args[args.length - 1].toLowerCase(Locale.ROOT)))
                        .toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("plotinfo")
                    && (player.hasPermission("ethriaplotaddon.plotinfo.view")
                    || player.hasPermission("ethriaplotaddon.plotinfo.edit")
                    || player.hasPermission("ethriaplotaddon.plotinfo.list"))) {
                List<String> options = new ArrayList<>();
                if (player.hasPermission("ethriaplotaddon.plotinfo.edit")) {
                    options.add("add");
                }
                if (player.hasPermission("ethriaplotaddon.plotinfo.list")) options.add("list");
                return options.stream().filter(option -> option.startsWith(args[1].toLowerCase())).toList();
            } else if (args.length == 3 && args[0].equalsIgnoreCase("plotinfo")
                    && args[1].equalsIgnoreCase("list")
                    && player.hasPermission("ethriaplotaddon.plotinfo.list")) {
                return getOnlinePlayerCompletions(args[2]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> getOnlinePlayerCompletions(String partialName) {
        String prefix = partialName.toLowerCase(Locale.ROOT);
        return org.bukkit.Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> getEntityCompletions(Player player, String partialName) {
        Set<String> completions = new HashSet<>();
        String lowerPartial = partialName.toLowerCase();

        // Admin-spezifische Ergänzungen
        if (PermissionManager.hasAdminPermission(player)) {
            if ("all".startsWith(lowerPartial)) {
                completions.add("all");
            }
            if ("*".startsWith(lowerPartial)) {
                completions.add("*");
            }
        }

        // Standard Commands
        if ("help".startsWith(lowerPartial)) {
            completions.add("help");
        }

        if (PermissionManager.canReload(player) && "reload".startsWith(lowerPartial)) {
            completions.add("reload");
        }

        // Entity-Namen basierend auf Berechtigungen
        addEntityCompletions(player, lowerPartial, completions);

        // Sortiere und limitiere Ergebnisse
        return completions.stream()
            .sorted()
            .limit(50) // Begrenze auf 50 Ergebnisse für Performance
            .collect(Collectors.toList());
    }

    private void addEntityCompletions(Player player, String lowerPartial, Set<String> completions) {
        // Durchsuche alle Entity-Gruppen
        String[] groups = {"animals", "mobs", "entities", "vehicles", "items", "projectiles"};

        for (String group : groups) {
            // Prüfe ob Spieler Berechtigung für diese Gruppe hat
            if (!PermissionManager.canCountGroup(player, group)) {
                continue;
            }

            // Hole Entity-Namen aus der entsprechenden Gruppe
            addEntityNamesFromGroup(group, lowerPartial, completions);
        }

        // Füge auch spezifische Entity-Berechtigungen hinzu
        addSpecificEntityPermissions(player, lowerPartial, completions);
    }

    private void addEntityNamesFromGroup(String group, String lowerPartial, Set<String> completions) {
        // Hole alle EntityTypes für diese Gruppe
        Set<EntityType> groupTypes = EntityGroupManager.getGroupTypes(group);

        for (EntityType entityType : groupTypes) {
            String englishName = entityType.name().toLowerCase();

            // Englische Namen hinzufügen
            if (englishName.startsWith(lowerPartial)) {
                completions.add(englishName);
            }

            // Deutsche Übersetzungen hinzufügen
            String deutscherName = messageManager.getEntityName(englishName);
            if (deutscherName != null) {
                String lowerDeutsch = deutscherName.toLowerCase();
                if (lowerDeutsch.startsWith(lowerPartial)) {
                    completions.add(deutscherName);
                }
            }
        }
    }

    private void addSpecificEntityPermissions(Player player, String lowerPartial, Set<String> completions) {
        // Prüfe spezifische Entity-Berechtigungen
        for (EntityType entityType : EntityType.values()) {
            if (PermissionManager.canCountEntity(player, entityType)) {
                String englishName = entityType.name().toLowerCase();

                // Englische Namen
                if (englishName.startsWith(lowerPartial)) {
                    completions.add(englishName);
                }

                // Deutsche Namen
                String deutscherName = messageManager.getEntityName(englishName);
                if (deutscherName != null) {
                    String lowerDeutsch = deutscherName.toLowerCase();
                    if (lowerDeutsch.startsWith(lowerPartial)) {
                        completions.add(deutscherName);
                    }
                }
            }
        }
    }

    /**
     * Hilfsmethode für dynamische YAML-basierte Completions
     */
    private void addYamlEntityCompletions(String group, String lowerPartial, Set<String> completions) {
        try {
            // Diese Methode könnte erweitert werden um direkt aus der YAML zu lesen
            // Für jetzt verwenden wir die EntityGroupManager Approach
        } catch (Exception e) {
            // Stille Fehlerbehandlung für Tab-Completion
        }
    }

    /**
     * Prüft ob Spieler mindestens eine Plugin-Berechtigung hat
     */
    private boolean hasAnyPermission(Player player) {
        // Admin hat immer Zugriff
        if (PermissionManager.hasAdminPermission(player)) {
            return true;
        }

        // Count-Berechtigungen
        if (PermissionManager.canCountOnOwnPlots(player) ||
            PermissionManager.canCountOnOtherPlots(player)) {
            return true;
        }

        // Plot-Check Berechtigung
        if (PermissionManager.canCheckPlots(player)) {
            return true;
        }

        // Reload-Berechtigung
        if (PermissionManager.canReload(player)) {
            return true;
        }

        if (player.hasPermission("ethriaplotaddon.plotinfo.view")
                || player.hasPermission("ethriaplotaddon.plotinfo.edit")) {
            return true;
        }

        return false;
    }
}
