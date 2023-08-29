package xyz.destiall.sgcraftlink.managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.PluginMessageProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

public class VelocityPluginMessager implements PluginMessageProvider {
    private final SGCraftLinkVelocity plugin;

    public VelocityPluginMessager(SGCraftLinkVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public OutputProvider getOutputProvider() {
        return new VelocityOutput();
    }

    @Override
    public PluginProvider getPlugin() {
        return plugin;
    }

    @Override
    public boolean sendPluginMessage(String server, byte[] data) {
        RegisteredServer srv = plugin.getProxy().getAllServers().stream().filter(s -> s.getServerInfo().getName().equalsIgnoreCase(server)).findFirst().orElse(null);
        if (srv == null)
            return false;

        return srv.sendPluginMessage(SGCraftLinkVelocity.CHANNEL, data);
    }

    @Override
    public boolean sendPluginMessage(byte[] data) {
        for (RegisteredServer server : plugin.getProxy().getAllServers()) {
            server.sendPluginMessage(SGCraftLinkVelocity.CHANNEL, data);
        }
        return true;
    }

    static class VelocityOutput extends OutputProvider {
        private final ByteArrayDataOutput out;
        public VelocityOutput() {
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
