package xyz.destiall.sgcraftlink.velocityproviders;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.UUID;

public class VelocityPlayer extends PlayerProvider {
    private final Player player;

    public VelocityPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void sendData(String channel, byte[] data) {
        player.sendPluginMessage(MinecraftChannelIdentifier.from(channel), data);
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public UUID getUUID() {
        return player.getUniqueId();
    }

    @Override
    public String getServer() {
        ServerConnection server = player.getCurrentServer().orElse(null);
        return server != null ? server.getServerInfo().getName() : null;
    }

    @Override
    public boolean hasPermission(String perms) {
        return player.hasPermission(perms);
    }

    @Override
    public void sendMessage(String msg) {
        player.sendMessage(Component.text(ColorProvider.of(msg)));
    }

    @Override
    public void sendMessageComponent(ComponentProvider<?>... componentProviders) {
        for (ComponentProvider<?> component : componentProviders) {
            player.sendMessage(((VelocityTextComponent) component).getComponent());
        }
    }

    @Override
    public String getPrefix() {
        String prefix = super.getPrefix();
        return prefix == null ? null : LegacyComponentSerializer.legacyAmpersand().deserialize(prefix).content();
    }

    @Override
    public String getSuffix() {
        String suffix = super.getSuffix();
        return suffix == null ? null : LegacyComponentSerializer.legacyAmpersand().deserialize(suffix).content();
    }

    @Override
    public boolean isOnline() {
        return player.isActive();
    }

    @Override
    public String getIP() {
        return player.getRemoteAddress().getHostName();
    }
}
