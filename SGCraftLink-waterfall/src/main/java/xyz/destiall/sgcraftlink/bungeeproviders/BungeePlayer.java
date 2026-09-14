package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.Arrays;
import java.util.UUID;

public class BungeePlayer extends PlayerProvider {
    private final ProxiedPlayer player;

    public BungeePlayer(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public void sendData(String channel, byte[] data) {
        player.sendData(channel, data);
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public UUID getUUID() {
        return player.getUniqueId();
    }

    @Override
    public String getServer() {
        return player.getServer().getInfo().getName();
    }

    @Override
    public boolean hasPermission(String perms) {
        return player.hasPermission(perms);
    }

    @Override
    public void sendMessage(String msg) {
        player.sendMessage(new TextComponent(ColorProvider.of(msg)));
    }

    @Override
    public void sendMessageComponent(ComponentProvider<?>... componentProviders) {
        player.sendMessage(Arrays.stream(componentProviders).map(componentProvider -> ((BungeeTextComponent) componentProvider).getComponent()).toArray(TextComponent[]::new));
    }

    @Override
    public boolean isOnline() {
        return player.isConnected();
    }

    @Override
    public String getIP() {
        return player.getAddress().getAddress().getHostAddress();
    }

    @Override
    public void kick(String reason) {
        player.disconnect(TextComponent.fromLegacy(ChatColor.translateAlternateColorCodes('&', reason)));
    }

    @Override
    public String getSuffix() {
        String suffix = super.getSuffix();
        return suffix == null ? null : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', suffix));
    }

    @Override
    public String getPrefix() {
        String prefix = super.getPrefix();
        return prefix == null ? null : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', prefix));
    }
}
