package xyz.destiall.sgcraftlink.velocityproviders;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.UUID;

public class VelocityConsolePlayer extends PlayerProvider {
    private final ProxyServer server;

    public VelocityConsolePlayer(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void sendData(String channel, byte[] data) {
    }

    @Override
    public String getName() {
        return "Console";
    }

    @Override
    public UUID getUUID() {
        return null;
    }

    @Override
    public String getServer() {
        return null;
    }

    @Override
    public boolean hasPermission(String perms) {
        return server.getConsoleCommandSource().hasPermission(perms);
    }

    @Override
    public void sendMessage(String msg) {
        server.getConsoleCommandSource().sendMessage(Component.text(ColorProvider.of(msg)));
    }

    @Override
    public void sendMessageComponent(ComponentProvider<?>... componentProviders) {
        for (ComponentProvider<?> component : componentProviders) {
            server.getConsoleCommandSource().sendMessage(((VelocityTextComponent) component).getComponent());
        }
    }

    @Override
    public String getIP() {
        return "localhost";
    }
}
