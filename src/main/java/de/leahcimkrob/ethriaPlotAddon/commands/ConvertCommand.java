package de.leahcimkrob.ethriaPlotAddon.commands;

import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Copies plot records between the configured SQLite and MySQL databases. */
public final class ConvertCommand implements SubcommandExecutor {
    private static final String PERMISSION = "ethriaplotaddon.admin.convert";
    private static final List<String> TYPES = List.of("sqlite", "mysql");

    private final EthriaPlotAddon plugin;

    public ConvertCommand(EthriaPlotAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(plugin.getMessageManager().getComponent("convert_usage", Map.of("usage", getUsage())));
            return true;
        }
        String from = args[0].toLowerCase(Locale.ROOT);
        String to = args[1].toLowerCase(Locale.ROOT);
        if (!TYPES.contains(from) || !TYPES.contains(to) || from.equals(to)) {
            player.sendMessage(plugin.getMessageManager().getComponent("convert_invalid_types"));
            return true;
        }

        try {
            plugin.getDatabaseManager().convert(from, to);
            player.sendMessage(plugin.getMessageManager().getComponent("convert_success", Map.of("from", from, "to", to)));
        } catch (SQLException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Datenbankkonvertierung fehlgeschlagen.", exception);
            player.sendMessage(plugin.getMessageManager().getComponent("convert_failed",
                    Map.of("error", exception.getMessage() == null ? "unknown" : exception.getMessage())));
        }
        return true;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission(PERMISSION);
    }

    @Override
    public String getDescription() {
        return "Überträgt Einträge zwischen SQLite und MySQL";
    }

    @Override
    public String getUsage() {
        return "/plotaddon convert <sqlite|mysql> <sqlite|mysql>";
    }
}
