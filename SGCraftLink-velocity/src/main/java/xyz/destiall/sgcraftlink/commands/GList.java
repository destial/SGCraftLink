package xyz.destiall.sgcraftlink.commands;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.Collection;
import java.util.stream.Collectors;

public class GList implements CommandProvider {
    private final SGCraftLinkVelocity plugin;
    public GList(SGCraftLinkVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(PlayerProvider player, String[] args) {
        if (!player.hasPermission("bungeecord.command.list"))
            return;

        for (RegisteredServer server : plugin.getProxy().getAllServers()) {
            Collection<Player> connected = server.getPlayersConnected();
            ComponentProvider<?> component = ComponentProvider.Builder
                    .newBuilder("[" + server.getServerInfo().getName() + "] ", ColorProvider.ChatColor.GREEN)
                    .append("(" + connected.size() + "): ", ColorProvider.ChatColor.YELLOW)
                    .append(connected.stream().map(Player::getUsername).collect(Collectors.joining(", ")), ColorProvider.ChatColor.WHITE)
                    .clickEvent(ComponentClickProvider.Action.RUN_COMMAND, "/server " + server.getServerInfo().getName())
                    .hoverEvent(ComponentHoverProvider.Action.SHOW_TEXT, "Click to join " + server.getServerInfo().getName(), ColorProvider.ChatColor.GREEN)
                    .build();
            player.sendMessageComponent(component);
        }
    }
}
