package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PMListener {
    protected final PluginProvider plugin;

    public PMListener(PluginProvider plugin) {
        this.plugin = plugin;
    }

    protected void onPluginMessage(String channel, byte[] data) {
        if (!channel.equals(SGCraftLinkCommon.CHANNEL)) return;

        try (ByteArrayInputStream stream = new ByteArrayInputStream(data); DataInputStream input = new DataInputStream(stream)) {
            String type = input.readUTF();
            if (type.equals(SGCraftLinkCommon.SET_FIRSTJOIN)) {
                String playerName = input.readUTF();
                long first = input.readLong();
                if (plugin.getPlaytimeProvider().setFirstJoin(playerName, first)) {
                    plugin.getLoggerProvider().info("Setting first join playtime of " + playerName + " to " + first);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
