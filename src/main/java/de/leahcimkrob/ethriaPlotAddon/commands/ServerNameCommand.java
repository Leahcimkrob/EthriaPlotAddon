package de.leahcimkrob.ethriaPlotAddon.commands;

import de.leahcimkrob.ethriaPlotAddon.EthriaPlotAddon;
import de.leahcimkrob.ethriaPlotAddon.util.PermissionManager;
import org.bukkit.entity.Player;

import java.util.Map;

/** Displays the automatically detected name of this Paper server. */
public final class ServerNameCommand implements SubcommandExecutor {
    private final EthriaPlotAddon plugin;

    public ServerNameCommand(EthriaPlotAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length != 0) {
            player.sendMessage(plugin.getMessageManager().getComponent("server_name_usage",
                    Map.of("usage", getUsage())));
            return true;
        }
        String serverName = de.leahcimkrob.ethriaPlotAddon.util.ServerNameResolver.resolve(plugin);
        player.sendMessage(plugin.getMessageManager().getComponent("server_name_current",
                Map.of("servername", serverName)));
        return true;
    }

    @Override
    public boolean hasPermission(Player player) {
        return PermissionManager.hasGeneralAdminPermission(player);
    }

    @Override
    public String getDescription() {
        return "Zeigt den automatisch erkannten Servernamen";
    }

    @Override
    public String getUsage() {
        return "/plotaddon server";
    }
}
