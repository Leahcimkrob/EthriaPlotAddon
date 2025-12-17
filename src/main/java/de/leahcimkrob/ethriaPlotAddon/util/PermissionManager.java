package de.leahcimkrob.ethriaPlotAddon.util;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class PermissionManager {

    // === ADMIN BERECHTIGUNGEN ===
    public static boolean hasAdminPermission(Player player) {
        return player.hasPermission("ethriaplotaddon.count.admin");
    }

    // === PLOT-ZUGRIFF ===
    public static boolean canCountOnOwnPlots(Player player) {
        return player.hasPermission("ethriaplotaddon.count.own");
    }

    public static boolean canCountOnOtherPlots(Player player) {
        return player.hasPermission("ethriaplotaddon.count.other");
    }

    // === ENTITY-BERECHTIGUNGEN ===
    public static boolean canCountEntity(Player player, EntityType entityType) {
        // Admin kann alles
        if (hasAdminPermission(player)) {
            return true;
        }

        String entityName = entityType.name().toLowerCase();

        // Spezifische Entity-Berechtigung prüfen
        if (player.hasPermission("ethriaplotaddon.count.entity." + entityName) ||
            player.hasPermission("ethriaplotaddoncount.entity.*")) {
            return true;
        }

        // Gruppen-Berechtigung prüfen
        String group = EntityGroupManager.getEntityGroup(entityType);
        return player.hasPermission("ethriaplotcount.group." + group) ||
               player.hasPermission("ethriaplotcount.group.*");
    }

    public static boolean canCountGroup(Player player, String group) {
        // Admin kann alles
        if (hasAdminPermission(player)) {
            return true;
        }

        // Gruppen-Berechtigung prüfen
        return player.hasPermission("ethriaplotaddon.count.group." + group.toLowerCase()) ||
               player.hasPermission("ethriaplotaddon.count.group.*");
    }

    // === BASIS-BERECHTIGUNGEN ===
    public static boolean hasBasePermission(Player player) {
        return player.hasPermission("ethriaplotcount.use");
    }

    public static boolean canReload(Player player) {
        return player.hasPermission("ethriaplotaddon.count.reload") ||
               hasAdminPermission(player);
    }

    // === HILFSMETHODEN ===
    public static String getPermissionError(Player player, EntityType entityType) {
        String entityName = entityType.name().toLowerCase();
        String group = EntityGroupManager.getEntityGroup(entityType);

        return "Du benötigst eine der folgenden Berechtigungen: " +
               "ethriaplotaddon.count.entity." + entityName + ", " +
               "ethriaplotaddon.count.group." + group + " oder " +
               "ethriaplotaddon.count.admin";
    }

    public static String getPlotAccessError(Player player) {
        if (!canCountOnOwnPlots(player) && !canCountOnOtherPlots(player)) {
            return "Du benötigst ethriaplotaddon.count.own oder ethriaplotcount.other Berechtigung!";
        }
        return "Du kannst nur auf deinen eigenen/getrusted/added Plots zählen (wenn der Owner online ist)!";
    }
    // === PLOT-CHECK BERECHTIGUNG ===
    public static boolean canCheckPlots(Player player) {
        return player.hasPermission("ethriaplotaddon.plotcheck.use");
    }
}
