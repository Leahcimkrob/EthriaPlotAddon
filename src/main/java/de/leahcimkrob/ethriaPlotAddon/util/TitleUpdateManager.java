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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
                actionBarMessage = "§7Nicht auf einem Plot";
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
        StringBuilder info = new StringBuilder();

        // Plot ID
        info.append("§6Plot: §e").append(plot.getId().toString()).append(" ");

        // Plot Owner Info
        UUID ownerUuid = plot.getOwner();
        if (ownerUuid == null) {
            info.append("§7(Kein Besitzer)");
        } else {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
            String ownerName = owner.getName();
            if (ownerName == null) {
                ownerName = "Unbekannt";
            }

            // Prüfe ob Owner online ist
            Player onlineOwner = Bukkit.getPlayer(ownerUuid);
            if (onlineOwner != null && onlineOwner.isOnline()) {
                info.append("§7Besitzer: §a").append(ownerName).append(" §7(§aOnline§7)");
            } else {
                // Owner ist offline
                long lastSeen = owner.getLastSeen();
                if (lastSeen == 0) {
                    info.append("§7Besitzer: §c").append(ownerName).append(" §7(§cNie online§7)");
                } else {
                    String timeAgo = formatTimeAgoShort(lastSeen);
                    info.append("§7Besitzer: §c").append(ownerName).append(" §7(§c").append(timeAgo).append("§7)");
                }
            }
        }

        return info.toString();
    }

    /**
     * Kurze Formatierung der Zeit seit letztem Online-Status für Title
     */
    private String formatTimeAgoShort(long lastSeenMillis) {
        Instant lastSeen = Instant.ofEpochMilli(lastSeenMillis);
        Instant now = Instant.now();

        long days = ChronoUnit.DAYS.between(lastSeen, now);
        long hours = ChronoUnit.HOURS.between(lastSeen, now) % 24;
        long minutes = ChronoUnit.MINUTES.between(lastSeen, now) % 60;

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
