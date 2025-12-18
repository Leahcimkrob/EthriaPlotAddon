package de.leahcimkrob.ethriaPlotAddon.util;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class PermissionManager {

    // === ALLGEMEINE ADMIN BERECHTIGUNGEN ===
    public static boolean hasGeneralAdminPermission(Player player) {
        return player.hasPermission("ethriaplotaddon.admin");
    }

    public static boolean hasCountAdminPermission(Player player) {
        return player.hasPermission("ethriaplotaddon.count.admin") || hasGeneralAdminPermission(player);
    }

    // Alte Methode für Rückwärtskompatibilität
    public static boolean hasAdminPermission(Player player) {
        return hasCountAdminPermission(player);
    }

    // === PLOT-ZUGRIFF ===
    public static boolean canCountOnOwnPlots(Player player) {
        return player.hasPermission("ethriaplotaddon.count.own") || hasGeneralAdminPermission(player);
    }

    public static boolean canCountOnOtherPlots(Player player) {
        return player.hasPermission("ethriaplotaddon.count.other") || hasGeneralAdminPermission(player);
    }

    // === ENTITY-BERECHTIGUNGEN ===
    public static boolean canCountEntity(Player player, EntityType entityType) {
        // Admin kann alles
        if (hasCountAdminPermission(player)) {
            return true;
        }

        String entityName = entityType.name().toLowerCase();

        // Spezifische Entity-Berechtigung prüfen
        if (player.hasPermission("ethriaplotaddon.count.entity." + entityName) ||
                player.hasPermission("ethriaplotaddon.count.entity.*")) {
            return true;
        }

        // Gruppen-Berechtigung prüfen
        String group = EntityGroupManager.getEntityGroup(entityType);
        return player.hasPermission("ethriaplotaddon.count.group." + group) ||
                player.hasPermission("ethriaplotaddon.count.group.*");
    }

    public static boolean canCountGroup(Player player, String group) {
        // Admin kann alles
        if (hasCountAdminPermission(player)) {
            return true;
        }

        // Gruppen-Berechtigung prüfen
        return player.hasPermission("ethriaplotaddon.count.group." + group.toLowerCase()) ||
                player.hasPermission("ethriaplotaddon.count.group.*");
    }

    // === BASIS-BERECHTIGUNGEN ===
    public static boolean hasBasePermission(Player player) {
        return player.hasPermission("ethriaplotaddon.count.use") || hasGeneralAdminPermission(player);
    }

    public static boolean canReload(Player player) {
        return player.hasPermission("ethriaplotaddon.count.reload") ||
                hasGeneralAdminPermission(player);
    }

    // === PLOT-CHECK BERECHTIGUNG ===
    public static boolean canCheckPlots(Player player) {
        return player.hasPermission("ethriaplotaddon.plotcheck.use") || hasGeneralAdminPermission(player);
    }

    // === HILFSMETHODEN ===
    public static String getPermissionError(Player player, EntityType entityType) {
        String entityName = entityType.name().toLowerCase();
        String group = EntityGroupManager.getEntityGroup(entityType);

        return "Du benötigst eine der folgenden Berechtigungen: " +
                "ethriaplotaddon.count.entity." + entityName + ", " +
                "ethriaplotaddon.count.group." + group + ", " +
                "ethriaplotaddon.count.admin oder ethriaplotaddon.admin";
    }

    public static String getPlotAccessError(Player player) {
        if (!canCountOnOwnPlots(player) && !canCountOnOtherPlots(player)) {
            return "Du benötigst ethriaplotaddon.count.own, ethriaplotaddon.count.other oder ethriaplotaddon.admin Berechtigung!";
        }
        return "Du kannst nur auf deinen eigenen/getrusted/added Plots zählen (wenn der Owner online ist)!";
    }
}
