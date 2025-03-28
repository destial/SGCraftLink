package xyz.destiall.sgcraftlink.commands;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

        Map<RegisteredServer, ServerPing> pings = new HashMap<>();
        for (RegisteredServer server : plugin.getProxy().getAllServers()) {
            try {
                pings.put(server, server.ping(PingOptions.DEFAULT).exceptionally((e) -> null).get(1, TimeUnit.SECONDS));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<RegisteredServer, ServerPing> entry : pings.entrySet()) {
            RegisteredServer server = entry.getKey();
            ServerPing ping = entry.getValue();
            Collection<Player> connected = server.getPlayersConnected();
            ComponentProvider<?> component = ComponentProvider.Builder
                    .newBuilder("[" + server.getServerInfo().getName() + "] ", ping != null ? ColorProvider.ChatColor.GREEN : ColorProvider.ChatColor.RED)
                    .append("(" + connected.size() + "): ", ColorProvider.ChatColor.YELLOW)
                    .append(connected.stream().map(Player::getUsername).collect(Collectors.joining(", ")), ColorProvider.ChatColor.WHITE)
                    .clickEvent(ComponentClickProvider.Action.RUN_COMMAND, "/server " + server.getServerInfo().getName())
                    .hoverEvent(ComponentHoverProvider.Action.SHOW_TEXT, "Click to join " + server.getServerInfo().getName(), ColorProvider.ChatColor.GREEN)
                    .build();
            player.sendMessageComponent(component);
        }
    }
}
