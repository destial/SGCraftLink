package xyz.destiall.sgcraftlink.velocityproviders;

import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.OfflinePlayerProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.UUID;

public class VelocityOfflinePlayer extends PlayerProvider implements OfflinePlayerProvider {
    private final String username;

    public VelocityOfflinePlayer(String username) {
        this.username = username;
    }

    @Override
    public VelocityPlayer checkOnline(PluginProvider plugin) {
        PlayerProvider provider = plugin.getPlayer(username);
        if (provider instanceof VelocityPlayer) {
            return (VelocityPlayer) provider;
        }
        return null;
    }

    @Override
    public void sendData(String channel, byte[] data) {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        if (online == null)
            return;
        online.sendData(channel, data);
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public UUID getUUID() {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getUUID() : null;
    }

    @Override
    public String getServer() {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getServer() : null;
    }

    @Override
    public boolean hasPermission(String perms) {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null && online.hasPermission(perms);
    }

    @Override
    public void sendMessage(String msg) {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        if (online == null)
            return;
        online.sendMessage(msg);
    }

    @Override
    public void sendMessageComponent(ComponentProvider<?>... componentProviders) {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        if (online == null)
            return;
        online.sendMessageComponent(componentProviders);
    }

    @Override
    public String getPrefix() {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getPrefix() : null;
    }

    @Override
    public String getSuffix() {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getSuffix() : null;
    }

    @Override
    public boolean isOnline() {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null && online.isOnline();
    }

    @Override
    public String getIP() {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getIP() : null;
    }

    @Override
    public void kick(String reason) {
        VelocityPlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        if (online == null)
            return;
        online.kick(reason);
    }
}
