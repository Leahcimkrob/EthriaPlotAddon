package de.leahcimkrob.ethriaPlotAddon.integration;

import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Integration für Vanilla Minecraft ActionBar
 * Keine externen Plugin-Dependencies erforderlich
 */
public class ActionBarIntegration {

    private final EthriaPlotAddon plugin;

    public ActionBarIntegration(EthriaPlotAddon plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("Vanilla ActionBar Integration aktiviert - keine externen Dependencies benötigt");
    }

    /**
     * Sendet eine ActionBar Message an den Spieler
     */
    public boolean sendActionBarMessage(Player player, String message) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        try {
            // Konvertiere Legacy-Format zu Adventure Component
            Component component = LegacyComponentSerializer.legacySection().deserialize(message);

            // Sende ActionBar Message
            player.sendActionBar(component);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Senden der ActionBar Message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Entfernt die ActionBar Message (sendet leere Message)
     */
    public boolean clearActionBar(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        try {
            player.sendActionBar(Component.empty());
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Entfernen der ActionBar Message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prüft ob ActionBar verfügbar ist (immer true bei Paper/Spigot)
     */
    public static boolean isActionBarAvailable() {
        return true; // ActionBar ist immer in Paper/Spigot verfügbar
    }
}
