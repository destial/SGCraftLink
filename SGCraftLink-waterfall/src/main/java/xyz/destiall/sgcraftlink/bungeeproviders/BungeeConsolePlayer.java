package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;

import java.util.Arrays;
import java.util.UUID;

public class BungeeConsolePlayer extends PlayerProvider {
    private final ProxyServer server;

    public BungeeConsolePlayer(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void sendData(String channel, byte[] data) {}

    @Override
    public String getName() {
        return server.getConsole().getName();
    }

    @Override
    public UUID getUUID() {
        return null;
    }

    @Override
    public String getServer() {
        return "global";
    }

    @Override
    public boolean hasPermission(String perms) {
        return server.getConsole().hasPermission(perms);
    }

    @Override
    public void sendMessage(String msg) {
        server.getConsole().sendMessage(new TextComponent(ColorProvider.of(msg)));
    }

    @Override
    public void sendMessageComponent(ComponentProvider<?>... componentProviders) {
        server.getConsole().sendMessage(Arrays.stream(componentProviders).map(componentProvider -> ((BungeeTextComponent) componentProvider).getComponent()).toArray(TextComponent[]::new));
    }
}
