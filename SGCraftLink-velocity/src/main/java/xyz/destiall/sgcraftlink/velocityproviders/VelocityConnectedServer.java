package xyz.destiall.sgcraftlink.velocityproviders;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.ServerProvider;

import java.util.Collection;
import java.util.stream.Collectors;

public class VelocityConnectedServer implements ServerProvider {
    private final RegisteredServer server;
    public VelocityConnectedServer(RegisteredServer server) {
        this.server = server;
    }

    @Override
    public String getName() {
        return server.getServerInfo().getName();
    }

    @Override
    public Collection<PlayerProvider> getPlayers() {
        return server.getPlayersConnected().stream().map(VelocityPlayer::new).collect(Collectors.toList());
    }
}
