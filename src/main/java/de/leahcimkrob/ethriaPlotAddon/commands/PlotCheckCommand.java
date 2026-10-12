package de.leahcimkrob.ethriaPlotAddon.commands;

import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration;
import de.leahcimkrob.ethriaPlotAddon.util.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Subcommand zum Prüfen des Plot-Inhabers und seiner letzten Online-Zeit
 */
public class PlotCheckCommand implements SubcommandExecutor {

    private final EthriaPlotAddon plugin;

    public PlotCheckCommand(EthriaPlotAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        // Prüfe auf "on" oder "off" Parameter für Live-Updates (nur Admin)
        if (args.length > 0) {
            String subArg = args[0].toLowerCase();
            
            if ("on".equals(subArg)) {
                // Prüfe Admin-Berechtigung für Live-Updates
                if (!PermissionManager.hasAdminPermission(player)) {
                    player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                    return true;
                }
                return handleLiveUpdatesOn(player);
            } else if ("off".equals(subArg)) {
                // Prüfe Admin-Berechtigung für Live-Updates
                if (!PermissionManager.hasAdminPermission(player)) {
                    player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                    return true;
                }
                return handleLiveUpdatesOff(player);
            }
        }
        
        // Standard Plot-Check ohne Live-Updates (normale Permission)
        return executeStandardCheck(player, args);
    }

    /**
     * Aktiviert Live-Updates (/plotaddon check on)
     */
    private boolean handleLiveUpdatesOn(Player player) {
        // Prüfe ob Plugin korrekt initialisiert ist
        if (plugin.getTitleUpdateManager() == null) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_not_ready"));
            return true;
        }

        // ActionBar ist immer verfügbar - keine CMI-Prüfung nötig

        // Prüfe ob bereits aktiv
        if (plugin.getTitleUpdateManager().hasActiveUpdates(player)) {
            String message = plugin.getMessageManager().getMessage("plotcheck_live_already_enabled");
            if (message == null) {
                message = plugin.getMessageManager().getMessage("plotcheck_live_already_enabled");
            }
            player.sendMessage(message);
            return true;
        }

        // Starte Updates
        try {
            if (plugin.getTitleUpdateManager().startUpdates(player)) {
                String message = plugin.getMessageManager().getMessage("plotcheck_live_enabled");
                if (message == null) {
                    message = plugin.getMessageManager().getMessage("plotcheck_live_enabled");
                }
                player.sendMessage(message);
                
                if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugEnabled() && plugin.getDebugLogger() != null) {
                    plugin.getDebugLogger().debug("Plot-Info Updates für Spieler %s aktiviert", player.getName());
                }
            } else {
                player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_update_error"));
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_update_failed",
                    Map.of("error", e.getMessage() == null ? "unknown" : e.getMessage())));
            plugin.getLogger().warning("Fehler in PlotCheckCommand (on): " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Deaktiviert Live-Updates (/plotaddon check off)
     */
    private boolean handleLiveUpdatesOff(Player player) {
        // Prüfe ob Plugin korrekt initialisiert ist
        if (plugin.getTitleUpdateManager() == null) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_not_ready"));
            return true;
        }

        // ActionBar ist immer verfügbar - keine CMI-Prüfung nötig

        // Prüfe ob aktiv
        if (!plugin.getTitleUpdateManager().hasActiveUpdates(player)) {
            String message = plugin.getMessageManager().getMessage("plotcheck_live_already_disabled");
            if (message == null) {
                message = plugin.getMessageManager().getMessage("plotcheck_live_already_disabled");
            }
            player.sendMessage(message);
            return true;
        }

        // Stoppe Updates
        try {
            if (plugin.getTitleUpdateManager().stopUpdates(player)) {
                String message = plugin.getMessageManager().getMessage("plotcheck_live_disabled");
                if (message == null) {
                    message = plugin.getMessageManager().getMessage("plotcheck_live_disabled");
                }
                player.sendMessage(message);
                
                if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebugEnabled() && plugin.getDebugLogger() != null) {
                    plugin.getDebugLogger().debug("Plot-Info Updates für Spieler %s deaktiviert", player.getName());
                }
            } else {
                player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_disable_error"));
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_disable_failed",
                    Map.of("error", e.getMessage() == null ? "unknown" : e.getMessage())));
            plugin.getLogger().warning("Fehler in PlotCheckCommand (off): " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Standard Plot-Check Funktionalität
     */
    private boolean executeStandardCheck(Player player, String[] args) {
        // Prüfe ob Spieler auf einem Plot ist
        Plot plot = PlotSquaredIntegration.getPlayerPlot(player);
        if (plot == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("not_on_plot"));
            return true;
        }

        // Debug-Info
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().debug("PlotCheck für Plot: %s in Welt %s",
                plot.getId().toString(), plot.getWorldName());
        }

        // Hole Plot-Owner
        UUID ownerUuid = plot.getOwner();
        if (ownerUuid == null) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_no_owner"));
            return true;
        }

        // Hole OfflinePlayer für Owner
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
        String ownerName = owner.getName();
        if (ownerName == null) {
            ownerName = "Unbekannt";
        }

        // Prüfe ob Owner online ist
        Player onlineOwner = Bukkit.getPlayer(ownerUuid);
        if (onlineOwner != null && onlineOwner.isOnline()) {
            // Owner ist online
            Map<String, String> replacements = new HashMap<>();
            replacements.put("owner", ownerName);
            replacements.put("plotId", plot.getId().toString());

            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_owner_online", replacements));
        } else {
            // Owner ist offline - zeige letzte Online-Zeit
            long lastSeen = owner.getLastSeen();

            Map<String, String> replacements = new HashMap<>();
            replacements.put("owner", ownerName);
            replacements.put("plotId", plot.getId().toString());

            if (lastSeen == 0) {
                // Spieler war noch nie online oder unbekannt
                replacements.put("lastSeen", "nie");
                player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_owner_never", replacements));
            } else {
                // Berechne Zeit seit letztem Online-Status
                String timeAgo = formatTimeAgo(lastSeen);
                replacements.put("lastSeen", timeAgo);

                player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_owner_offline", replacements));
            }
        }

        // Zusätzliche Plot-Informationen falls erwünscht
        if (args.length > 0 && args[0].equalsIgnoreCase("-v")) {
            showExtendedPlotInfo(player, plot, ownerName);
        }

        return true;
    }

    @Override
    public boolean hasPermission(Player player) {
        return PermissionManager.canCheckPlots(player) || PermissionManager.hasAdminPermission(player);
    }

    @Override
    public String getDescription() {
        return "Zeigt Plot-Informationen oder aktiviert/deaktiviert Live-Updates (on/off nur Admin)";
    }

    @Override
    public String getUsage() {
        return "/plotaddon check [on|off (Admin)|-v]";
    }

    /**
     * Formatiert die Zeit seit dem letzten Online-Status
     */
    private String formatTimeAgo(long lastSeenMillis) {
        Instant lastSeen = Instant.ofEpochMilli(lastSeenMillis);
        Instant now = Instant.now();

        long days = ChronoUnit.DAYS.between(lastSeen, now);
        long hours = ChronoUnit.HOURS.between(lastSeen, now) % 24;
        long minutes = ChronoUnit.MINUTES.between(lastSeen, now) % 60;

        if (days > 0) {
            if (days == 1) {
                return String.format("vor %d Tag", days);
            } else {
                return String.format("vor %d Tagen", days);
            }
        } else if (hours > 0) {
            if (hours == 1) {
                return String.format("vor %d Stunde", hours);
            } else {
                return String.format("vor %d Stunden", hours);
            }
        } else if (minutes > 0) {
            if (minutes == 1) {
                return String.format("vor %d Minute", minutes);
            } else {
                return String.format("vor %d Minuten", minutes);
            }
        } else {
            return "vor wenigen Sekunden";
        }
    }

    /**
     * Zeigt erweiterte Plot-Informationen
     */
    private void showExtendedPlotInfo(Player player, Plot plot, String ownerName) {
        player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_extended_title"));
        player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_extended_id", Map.of("plotId", plot.getId().toString())));
        player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_extended_owner", Map.of("owner", ownerName)));
        player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_extended_world", Map.of("world", plot.getWorldName())));

        // Trusted/Added Spieler
        if (!plot.getTrusted().isEmpty() || !plot.getMembers().isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_members_title"));

            // Trusted Spieler
            for (UUID trustedUuid : plot.getTrusted()) {
                OfflinePlayer trusted = Bukkit.getOfflinePlayer(trustedUuid);
                String trustedName = trusted.getName();
                if (trustedName != null) {
                    String status = trusted.isOnline() ? "<green>online" : "<red>offline";
                    player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_member",
                            Map.of("name", trustedName, "role", "trusted", "status", status)));
                }
            }

            // Added Spieler (Members)
            for (UUID memberUuid : plot.getMembers()) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(memberUuid);
                String memberName = member.getName();
                if (memberName != null) {
                    String status = member.isOnline() ? "<green>online" : "<red>offline";
                    player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_member",
                            Map.of("name", memberName, "role", "member", "status", status)));
                }
            }
        }

        // Merge-Informationen
        if (plot.isMerged()) {
            player.sendMessage(plugin.getMessageManager().getComponent("plotcheck_merged"));
        }
    }
}
