package de.leahcimkrob.ethriaPlotAddon.commands;

import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Safely merges Plotinfo data from one configured database into the other. */
public final class PlotInfoImportCommand implements SubcommandExecutor {
    private static final String PERMISSION = "ethriaplotaddon.admin.databaseimport";
    private static final List<String> TYPES = List.of("sqlite", "mysql");
    private final EthriaPlotAddon plugin;

    public PlotInfoImportCommand(EthriaPlotAddon plugin) { this.plugin = plugin; }

    @Override public boolean execute(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(plugin.getMessageManager().getComponent("convert_usage", Map.of("usage", getUsage())));
            return true;
        }
        String from = args[0].toLowerCase(Locale.ROOT), to = args[1].toLowerCase(Locale.ROOT);
        if (!TYPES.contains(from) || !TYPES.contains(to) || from.equals(to)) {
            player.sendMessage(plugin.getMessageManager().getComponent("convert_invalid_types"));
            return true;
        }
        try {
            var result = plugin.getDatabaseManager().importBetween(from, to);
            player.sendMessage(plugin.getMessageManager().getComponent("plotinfo_import_success", Map.of(
                    "from", from, "to", to, "inserted", Integer.toString(result.inserted()),
                    "skipped", Integer.toString(result.skipped()))));
        } catch (SQLException | IllegalArgumentException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Plotinfo-Import fehlgeschlagen.", exception);
            player.sendMessage(plugin.getMessageManager().getComponent("convert_failed",
                    Map.of("error", exception.getMessage() == null ? "unknown" : exception.getMessage())));
        }
        return true;
    }

    @Override public boolean hasPermission(Player player) { return player.hasPermission(PERMISSION); }
    @Override public String getDescription() { return "Führt Plotinfo-Daten sicher zwischen SQLite und MySQL zusammen"; }
    @Override public String getUsage() { return "/plotaddon plotinfoimport <sqlite|mysql> <sqlite|mysql>"; }
}
