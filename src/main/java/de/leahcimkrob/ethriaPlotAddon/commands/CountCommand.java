package de.leahcimkrob.ethriaPlotAddon.commands;

import com.plotsquared.core.plot.Plot;
import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import de.leahcimkrob.ethriaPlotAddon.integration.PlotSquaredIntegration;
import de.leahcimkrob.ethriaPlotAddon.util.EntityCounter;
import de.leahcimkrob.ethriaPlotAddon.util.PermissionManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Subcommand für das Zählen von Entities auf Plots
 */
public class CountCommand implements SubcommandExecutor {

    private final EthriaPlotAddon plugin;

    public CountCommand(EthriaPlotAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getComponent("count_usage"));
            return true;
        }

        String entityTypeStr = args[0].toLowerCase();

        // Handle all/star command (nur für Admins)
        if (entityTypeStr.equals("all") || entityTypeStr.equals("*")) {
            if (!PermissionManager.hasAdminPermission(player)) {
                player.sendMessage(plugin.getMessageManager().getComponent("no_permission"));
                return true;
            }
            return handleCountAllEntities(player);
        }

        // Handle entity counting
        return handleEntityCount(player, entityTypeStr);
    }

    @Override
    public boolean hasPermission(Player player) {
        // Admin hat immer Berechtigung
        if (PermissionManager.hasAdminPermission(player)) {
            return true;
        }

        // Plot-Zugriff prüfen
        if (PermissionManager.canCountOnOwnPlots(player) || PermissionManager.canCountOnOtherPlots(player)) {
            return true;
        }

        // Entity-spezifische Berechtigungen prüfen
        if (player.hasPermission("ethriaplotaddon.count.entity.*")) {
            return true;
        }

        // Gruppen-Berechtigungen prüfen
        if (player.hasPermission("ethriaplotaddon.count.group.*")) {
            return true;
        }

        // Einzelne Gruppen prüfen (vereinfacht)
        return player.hasPermission("ethriaplotaddon.count.group.animals") ||
               player.hasPermission("ethriaplotaddon.count.group.mobs") ||
               player.hasPermission("ethriaplotaddon.count.group.entities") ||
               player.hasPermission("ethriaplotaddon.count.group.vehicles") ||
               player.hasPermission("ethriaplotaddon.count.group.items") ||
               player.hasPermission("ethriaplotaddon.count.group.projectiles");
    }

    @Override
    public String getDescription() {
        return "Zählt Entities auf dem aktuellen Plot";
    }

    @Override
    public String getUsage() {
        return "/plotaddon count <entity|all>";
    }

    private boolean handleEntityCount(Player player, String entityTypeStr) {
        // Debug: Prüfe ob Spieler auf einem Plot ist
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().debug("Prüfe Plot-Zugriff für Spieler: %s (Entity: %s)",
                player.getName(), entityTypeStr);
        }

        Plot plot = PlotSquaredIntegration.getPlayerPlot(player);
        if (plot == null) {
            player.sendMessage(plugin.getMessageManager().getComponent("not_on_plot"));
            return true;
        }

        // Debug: Zeige Plot-Info
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().debug("Plot gefunden: %s in Welt %s",
                plot.getId().toString(), plot.getWorldName());
        }

        // Prüfe Plot-Zugriff
        if (!PlotSquaredIntegration.hasPlotAccess(player, plot)) {
            String accessMessage = !PermissionManager.canCountOnOwnPlots(player)
                    && !PermissionManager.canCountOnOtherPlots(player)
                    ? "no_plot_permission" : "plot_access_denied";
            player.sendMessage(plugin.getMessageManager().getComponent(accessMessage));
            return true;
        }

        // NEUE LOGIK: Versuche deutsche Namen zu englischen zu konvertieren
        String originalInput = entityTypeStr;
        String englishEntityType = plugin.getMessageManager().getEnglishEntityName(entityTypeStr);

        // Nur bei Debug-Modus loggen
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().debug("Input: '%s' -> Konvertiert zu: '%s'",
                originalInput, englishEntityType);
        }

        // Validiere Entity-Typ (mit englischem Namen)
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(englishEntityType.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("entityType", originalInput);
            player.sendMessage(plugin.getMessageManager().getComponent("invalid_entity_type", replacements));
            return true;
        }

        String entityName = plugin.getMessageManager().getEntityName(englishEntityType);

        // Prüfe Entity-Berechtigung
        if (!PermissionManager.canCountEntity(player, entityType)) {
            player.sendMessage(plugin.getMessageManager().getComponent("no_entity_permission",
                    Map.of("entitytype", entityName)));
            return true;
        }

        // Hole alle Plots die gezählt werden sollen
        Set<Plot> plotsToCount = PlotSquaredIntegration.getPlotsToCount(
            plot, plugin.getConfigManager().shouldIncludeMergedPlots()
        );

        // Debug: Merge-Informationen loggen (nur einmal am Anfang)
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().debug("Spieler %s verwendet Befehl '/plotcount %s' auf Plot %s",
                    player.getName(), entityTypeStr, plot.getId().toString());
            PlotSquaredIntegration.debugMergeInfo(plotsToCount, plugin.getDebugLogger());

            // Debug: Zeige Merged-Plot Info
            if (plugin.getConfigManager().shouldIncludeMergedPlots() && plotsToCount.size() > 1) {
                plugin.getDebugLogger().debug("Inklusive %d gemergete(r) Plot(s)", plotsToCount.size() - 1);
            }
        }

        // Zeige angemessene Zähl-Nachricht
        // Verwende deutsche Übersetzung für Anzeige (vom englischen Namen)
        Map<String, String> countingMsg = new HashMap<>();
        countingMsg.put("entityType", entityName);

        if (plotsToCount.size() > 1) {
            countingMsg.put("plotCount", String.valueOf(plotsToCount.size()));
            player.sendMessage(plugin.getMessageManager().getComponent("counting_entities_large", countingMsg));
        } else {
            player.sendMessage(plugin.getMessageManager().getComponent("counting_entities", countingMsg));
        }

        // Führe Zählung durch (synchron, da Entity-Zugriff Main-Thread erfordert)
        EntityCounter.CountResult result = EntityCounter.countWithDetails(
            plotsToCount, entityType, plugin.getConfigManager(), plugin.getDebugLogger()
        );

        // Zeige Ergebnis
        displayCountResult(player, result, entityName, plotsToCount.size());

        return true;
    }

    /**
     * Zählt alle Entities für Admins
     */
    private boolean handleCountAllEntities(Player player) {
        // Debug: Prüfe ob Spieler auf einem Plot ist
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().debug("Prüfe Plot-Zugriff für Spieler: %s (plotcount all)", player.getName());
        }

        Plot plot = PlotSquaredIntegration.getPlayerPlot(player);
        if (plot == null) {
            player.sendMessage(plugin.getMessageManager().getComponent("not_on_plot"));
            return true;
        }

        // Debug: Zeige Plot-Info
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getDebugLogger().debug("Plot gefunden für plotcount all: %s in Welt %s",
                plot.getId().toString(), plot.getWorldName());
        }

        // Hole alle Plots die gezählt werden sollen
        Set<Plot> plotsToCount = PlotSquaredIntegration.getPlotsToCount(
            plot, plugin.getConfigManager().shouldIncludeMergedPlots()
        );

        // Debug: Zeige Merged-Plot Info
        if (plugin.getConfigManager().shouldIncludeMergedPlots() && plotsToCount.size() > 1) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getDebugLogger().debug("plotcount all: Inklusive %d gemergete(r) Plot(s)", plotsToCount.size() - 1);
            }
        }

        // Zeige entsprechenden Header basierend auf Plot-Count
        if (plugin.getConfigManager().shouldIncludeMergedPlots() && plotsToCount.size() > 1) {
            Map<String, String> headerReplacements = new HashMap<>();
            headerReplacements.put("plotCount", String.valueOf(plotsToCount.size()));
            player.sendMessage(plugin.getMessageManager().getComponent("counting_all_header_merged", headerReplacements));
        } else {
            player.sendMessage(plugin.getMessageManager().getComponent("counting_all_header"));
        }

        // Zähle alle Entity-Typen
        Map<EntityType, Integer> entityCounts = new HashMap<>();
        int totalEntities = 0;

        // Durchsuche alle EntityTypes
        for (EntityType entityType : EntityType.values()) {
            // Überspringe technische Entities
            if (isSkippableEntityType(entityType)) {
                continue;
            }

            try {
                EntityCounter.CountResult result = EntityCounter.countWithDetails(
                    plotsToCount, entityType, plugin.getConfigManager(), plugin.getDebugLogger()
                );

                int count = result.getTotalCount();
                if (count > 0) {
                    entityCounts.put(entityType, count);
                    totalEntities += count;
                }
            } catch (Exception e) {
                // Stille Fehlerbehandlung für einzelne Entity-Typen
                plugin.getLogger().fine("Fehler beim Zählen von " + entityType + ": " + e.getMessage());
            }
        }

        // Zeige Ergebnisse
        if (entityCounts.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getComponent("counting_all_no_entities"));
        } else {
            // Sortiere nach Anzahl (absteigend)
            entityCounts.entrySet().stream()
                .sorted(Map.Entry.<EntityType, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    EntityType type = entry.getKey();
                    int count = entry.getValue();
                    String deutscherName = plugin.getMessageManager().getEntityName(type.name().toLowerCase());

                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow><name>: <white><count>",
                            Placeholder.unparsed("name", deutscherName), Placeholder.unparsed("count", Integer.toString(count))));
                });

            Map<String, String> totalReplacements = new HashMap<>();
            totalReplacements.put("total", String.valueOf(totalEntities));
            player.sendMessage(plugin.getMessageManager().getComponent("counting_all_total", totalReplacements));
        }

        return true;
    }

    /**
     * Prüft ob ein EntityType übersprungen werden soll
     */
    private boolean isSkippableEntityType(EntityType entityType) {
        // Überspringe Player und technische/problematische Entity-Typen
        String name = entityType.name();
        return name.equals("PLAYER") ||
               name.equals("UNKNOWN") ||
               name.equals("FISHING_HOOK") ||
               name.equals("LIGHTNING") ||
               name.equals("WEATHER") ||
               name.equals("COMPLEX_PART") ||
               name.equals("OTHER");
    }

    private void displayCountResult(Player player, EntityCounter.CountResult result, String entityName, int plotCount) {
        Map<String, String> replacements = new HashMap<>();

        // Berechne gefilterte Anzahl basierend auf Config
        int displayCount = result.getFilteredCount(plugin.getConfigManager());

        replacements.put("count", String.valueOf(displayCount));
        replacements.put("entityName", entityName);

        // Prüfe ob Limit erreicht wurde
        if (result.isLimitReached()) {
            replacements.put("limit", String.valueOf(plugin.getConfigManager().getMaxCountLimit()));
            player.sendMessage(plugin.getMessageManager().getComponent("count_limit_reached", replacements));
        }

        // Hauptergebnis
        if (plugin.getConfigManager().shouldIncludeMergedPlots() && plotCount > 1) {
            replacements.put("plotCount", String.valueOf(plotCount));
            player.sendMessage(plugin.getMessageManager().getComponent("count_result_merged", replacements));
        } else {
            player.sendMessage(plugin.getMessageManager().getComponent("count_result", replacements));
        }

        // Detaillierte Aufschlüsselung (nur wenn interessant)
        if (result.getInvisibleCount() > 0 || result.getFixedCount() > 0) {
            Map<String, String> detailReplacements = new HashMap<>();
            detailReplacements.put("count", String.valueOf(result.getTotalCount()));
            detailReplacements.put("entityName", entityName);
            detailReplacements.put("visible", String.valueOf(result.getVisibleCount()));
            detailReplacements.put("invisible", String.valueOf(result.getInvisibleCount()));
            detailReplacements.put("fixed", String.valueOf(result.getFixedCount()));

            player.sendMessage(plugin.getMessageManager().getComponent("count_result_detailed", detailReplacements));
        }
    }

    /**
     * Sendet eine Nachricht mit Prefix an den Player
     */
}
