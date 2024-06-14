package xyz.destiall.sgcraftlink.commands;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Send implements CommandProvider, TabProvider {
    private final SGCraftLinkVelocity plugin;
    public Send(SGCraftLinkVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(PlayerProvider player, String[] args) {
        if (!player.hasPermission("bungeecord.command.send"))
            return;

        // /send [player/all] [server]

        if (args == null || args.length != 2) {
            player.sendMessage(ColorProvider.of("&cUsage: /send [player|server|all] [server]"));
            return;
        }

        RegisteredServer server = plugin.getProxy().getServer(args[1]).orElse(null);
        if (server == null) {
            player.sendMessage(ColorProvider.of("&cInvalid target server!"));
            return;
        }

        Collection<Player> playersToSend;
        if (args[0].equalsIgnoreCase("all")) {
            playersToSend = plugin.getProxy().getAllPlayers();
        } else if (plugin.getProxy().getServer(args[1]).isPresent()) {
            RegisteredServer sourceServer = plugin.getProxy().getServer(args[1]).orElse(null);
            if (sourceServer == null) {
                player.sendMessage(ColorProvider.of("&cInvalid source server!"));
                return;
            }
            playersToSend = sourceServer.getPlayersConnected();
        } else {
            playersToSend = new ArrayList<>();
            Optional<Player> opt = plugin.getProxy().getPlayer(args[1]);
            opt.ifPresent(playersToSend::add);
        }

        List<CompletableFuture<ConnectionRequestBuilder.Result>> all = new ArrayList<>();
        for (Player p : playersToSend) {
            if (p.getUsername().equals(player.getName()))
                continue;

            all.add(p.createConnectionRequest(server).connect());
        }

        player.sendMessage(ColorProvider.of("&aSending " + playersToSend.size() + " to " + server.getServerInfo().getName()));
        CompletableFuture.allOf(all.toArray(new CompletableFuture[0])).whenComplete((v, ex) -> {
            List<ConnectionRequestBuilder.Status> results = all.stream().map(CompletableFuture::join).map(ConnectionRequestBuilder.Result::getStatus).collect(Collectors.toList());
            Map<ConnectionRequestBuilder.Status, Long> mapped = results.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
            player.sendMessage(ColorProvider.of("&aConnection Status: (" + results.size() + ")"));
            for (Map.Entry<ConnectionRequestBuilder.Status, Long> entry : mapped.entrySet()) {
                player.sendMessage(ColorProvider.of("&a" + entry.getKey().name() + ": " + entry.getValue()));
            }
        });
    }

    @Override
    public List<String> tab(PlayerProvider sender, String[] args) {
        return plugin.getPlayers().stream().map(PlayerProvider::getName).collect(Collectors.toList());
    }
}
