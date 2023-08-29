package xyz.destiall.sgcraftlink.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.config.ServerInfo;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.providers.PluginMessageProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

public class BungeePluginMessager implements PluginMessageProvider {
    private final SGCraftLinkWaterfall plugin;
    public BungeePluginMessager(SGCraftLinkWaterfall plugin) {
        this.plugin = plugin;
    }

    @Override
    public OutputProvider getOutputProvider() {
        return new BungeeOutput();
    }

    @Override
    public PluginProvider getPlugin() {
        return plugin;
    }

    @Override
    public boolean sendPluginMessage(String server, byte[] data) {
        return plugin.getProxy().getServerInfo(server).sendData(SGCraftLinkCommon.CHANNEL, data, true);
    }

    @Override
    public boolean sendPluginMessage(byte[] data) {
        for (ServerInfo server : plugin.getProxy().getServersCopy().values()) {
            server.sendData(SGCraftLinkCommon.CHANNEL, data);
        }
        return true;
    }

    static class BungeeOutput extends OutputProvider {
        private final ByteArrayDataOutput out;
        public BungeeOutput() {
            out = ByteStreams.newDataOutput();
        }

        @Override
        public void writeUTF(String input) {
            out.writeUTF(input);
        }

        @Override
        public byte[] toByteArray() {
            return out.toByteArray();
        }
    }
}
