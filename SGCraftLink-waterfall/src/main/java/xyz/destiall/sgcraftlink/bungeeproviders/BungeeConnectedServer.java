package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.config.ServerInfo;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.ServerProvider;

import java.util.Collection;
import java.util.stream.Collectors;

public class BungeeConnectedServer implements ServerProvider {
    private final ServerInfo server;
    public BungeeConnectedServer(ServerInfo server) {
        this.server = server;
    }

    @Override
    public String getName() {
        return server.getName();
    }

    @Override
    public Collection<PlayerProvider> getPlayers() {
        return server.getPlayers().stream().map(BungeePlayer::new).collect(Collectors.toList());
    }
}
