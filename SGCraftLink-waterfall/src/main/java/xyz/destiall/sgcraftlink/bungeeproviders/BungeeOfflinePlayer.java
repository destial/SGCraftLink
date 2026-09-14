package xyz.destiall.sgcraftlink.bungeeproviders;

import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.OfflinePlayerProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.UUID;

public class BungeeOfflinePlayer extends PlayerProvider implements OfflinePlayerProvider {
    private final String username;

    public BungeeOfflinePlayer(String username) {
        this.username = username;
    }

    @Override
    public BungeePlayer checkOnline(PluginProvider plugin) {
        PlayerProvider provider = plugin.getPlayer(username);
        if (provider instanceof BungeePlayer) {
            return (BungeePlayer) provider;
        }
        return null;
    }

    @Override
    public void sendData(String channel, byte[] data) {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
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
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getUUID() : null;
    }

    @Override
    public String getServer() {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getServer() : null;
    }

    @Override
    public boolean hasPermission(String perms) {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null && online.hasPermission(perms);
    }

    @Override
    public void sendMessage(String msg) {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        if (online == null)
            return;
        online.sendMessage(msg);
    }

    @Override
    public void sendMessageComponent(ComponentProvider<?>... componentProviders) {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        if (online == null)
            return;
        online.sendMessageComponent(componentProviders);
    }

    @Override
    public String getPrefix() {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getPrefix() : null;
    }

    @Override
    public String getSuffix() {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getSuffix() : null;
    }

    @Override
    public boolean isOnline() {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null && online.isOnline();
    }

    @Override
    public String getIP() {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        return online != null ? online.getIP() : null;
    }

    @Override
    public void kick(String reason) {
        BungeePlayer online = checkOnline(SGCraftLinkCommon.PROXY);
        if (online == null)
            return;
        online.kick(reason);
    }
}
