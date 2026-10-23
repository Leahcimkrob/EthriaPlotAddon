package de.leahcimkrob.ethriaPlotAddon.util;

import com.plotsquared.core.plot.Plot;
import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import de.leahcimkrob.ethriaPlotAddon.integration.ActionBarIntegration;
import de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager für automatische Plot-Info Updates in der ActionBar (Vanilla)
 */
public class TitleUpdateManager {

    private final EthriaPlotAddon plugin;
    private final ActionBarIntegration actionBarIntegration;
    private final Map<UUID, BukkitTask> activeTasks;

    public TitleUpdateManager(EthriaPlotAddon plugin) {
        this.plugin = plugin;
        this.actionBarIntegration = new ActionBarIntegration(plugin);
        this.activeTasks = new HashMap<>();
    }

    /**
     * Startet automatische Updates für einen Spieler
     */
    public boolean startUpdates(Player player) {
        if (player == null) {
            plugin.getLogger().warning("TitleUpdateManager.startUpdates: Spieler ist null");
            return false;
        }

        // ActionBar ist immer verfügbar
        if (!ActionBarIntegration.isActionBarAvailable()) {
            if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getDebugLogger().debug("ActionBar ist nicht verfügbar für Spieler %s", player.getName());
            }
            return false;
        }

        UUID playerUuid = player.getUniqueId();

        // Stoppe vorherige Updates falls vorhanden
        stopUpdates(player);

        try {
            // Hole Update-Intervall aus der Config (in Sekunden -> Ticks)
            int intervalSeconds = plugin.getConfigManager().getActionBarUpdateInterval();
            long intervalTicks = intervalSeconds * 20L; // Sekunden zu Ticks (20 Ticks = 1 Sekunde)

            // Starte neuen Update-Task mit konfigurierbarem Intervall
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    updateActionBarForPlayer(player);
                }
            }.runTaskTimer(plugin, 0L, intervalTicks);

            activeTasks.put(playerUuid, task);

            // Erste sofortige Aktualisierung
            updateActionBarForPlayer(player);

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Starten der ActionBar Updates für " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Stoppt automatische Updates für einen Spieler
     */
    public boolean stopUpdates(Player player) {
        if (player == null) {
            plugin.getLogger().warning("TitleUpdateManager.stopUpdates: Spieler ist null");
            return false;
        }

        UUID playerUuid = player.getUniqueId();
        BukkitTask task = activeTasks.remove(playerUuid);

        if (task != null) {
            try {
                task.cancel();
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Canceln der Task für " + player.getName() + ": " + e.getMessage());
            }
        }

        // Entferne ActionBar Message
        if (actionBarIntegration != null) {
            try {
                return actionBarIntegration.clearActionBar(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Entfernen der ActionBar Message für " + player.getName() + ": " + e.getMessage());
                return false;
            }
        }

        return true; // Task wurde gestoppt
    }

    /**
     * Prüft ob für einen Spieler Updates aktiv sind
     */
    public boolean hasActiveUpdates(Player player) {
        if (player == null) {
            return false;
        }
        return activeTasks.containsKey(player.getUniqueId());
    }

    /**
     * Aktualisiert die ActionBar für einen Spieler
     */
    private void updateActionBarForPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            if (player != null) {
                stopUpdates(player);
            }
            return;
        }

        try {
            // Hole aktuelles Plot
            Plot plot = PlotSquaredIntegration.getPlayerPlot(player);
            String actionBarMessage;

            if (plot == null) {
                actionBarMessage = renderMessage("plotcheck_actionbar_not_on_plot", Map.of());
            } else {
                actionBarMessage = generatePlotInfo(plot);
            }

            // Sende an ActionBar
            if (actionBarIntegration != null) {
                actionBarIntegration.sendActionBarMessage(player, actionBarMessage);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Update der ActionBar für " + player.getName() + ": " + e.getMessage());
            // Stoppe Updates bei wiederholten Fehlern
            stopUpdates(player);
        }
    }

    /**
     * Generiert die Plot-Info für die Title Message
     */
    private String generatePlotInfo(Plot plot) {
        UUID ownerUuid = plot.getOwner();
        if (ownerUuid == null) {
            return renderMessage("plotcheck_actionbar_no_owner", Map.of("plotid", plot.getId().toString()));
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
        String ownerName = owner.getName() == null ? "Unbekannt" : owner.getName();
        try {
            de.leahcimkrob.ethriaPlotAddon.database.PlotCheckDatabase.PlayerStatus status =
                    plugin.getPlotCheckDatabase().getStatus(ownerUuid, ownerName);
            String server = status.server() == null || status.server().isBlank() ? "Unbekannt" : status.server();
            Map<String, String> placeholders = Map.of(
                    "plotid", plot.getId().toString(),
                    "owner", ownerName,
                    "server", server);
            if (status.online()) {
                return renderMessage("plotcheck_actionbar_online", placeholders);
            }
            if (status.lastOnline() <= 0) {
                return renderMessage("plotcheck_actionbar_never", placeholders);
            }
            Map<String, String> offlinePlaceholders = new HashMap<>(placeholders);
            offlinePlaceholders.put("lastseen", formatTimeAgoShort(status.lastOnline()));
            return renderMessage("plotcheck_actionbar_offline", offlinePlaceholders);
        } catch (Exception e) {
            plugin.getLogger().warning("PlotCheck-Status für ActionBar konnte nicht aus der Datenbank gelesen werden: " + e.getMessage());
            return renderMessage("plotcheck_actionbar_unknown", Map.of(
                    "plotid", plot.getId().toString(), "owner", ownerName));
        }
    }

    private String renderMessage(String key, Map<String, String> placeholders) {
        return LegacyComponentSerializer.legacySection().serialize(
                plugin.getMessageManager().getComponent(key, placeholders, false));
    }

    /**
     * Kurze Formatierung der Zeit seit letztem Online-Status für Title
     */
    private String formatTimeAgoShort(long lastSeenMillis) {
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - lastSeenMillis);
        long minutesTotal = elapsedMillis / 60_000L;
        long hoursTotal = minutesTotal / 60L;
        long days = hoursTotal / 24L;
        long hours = hoursTotal % 24L;
        long minutes = minutesTotal % 60L;

        if (days > 0) {
            if (days >= 30) {
                long months = days / 30;
                return months + "Mo";
            } else if (days >= 7) {
                long weeks = days / 7;
                return weeks + "W";
            } else {
                return days + "d";
            }
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return "<1m";
        }
    }

    /**
     * Cleanup beim Plugin-Shutdown
     */
    public void shutdown() {
        // Stoppe alle aktiven Tasks
        for (Map.Entry<UUID, BukkitTask> entry : activeTasks.entrySet()) {
            BukkitTask task = entry.getValue();
            if (task != null) {
                task.cancel();
            }

            // Versuche ActionBar zu entfernen
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline() && actionBarIntegration != null) {
                actionBarIntegration.clearActionBar(player);
            }
        }
        activeTasks.clear();
    }
}
