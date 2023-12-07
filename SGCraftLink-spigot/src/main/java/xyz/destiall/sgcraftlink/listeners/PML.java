package xyz.destiall.sgcraftlink.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.CMIModule;
import xyz.destiall.sgcraftlink.modules.LuckPermsModule;

public class PML implements PluginMessageListener {
    private final SGCraftLinkSpigot plugin;

    public PML(SGCraftLinkSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        ByteArrayDataInput input = ByteStreams.newDataInput(message);
        String type = input.readUTF();
        switch (type) {
            case SGCraftLinkCommon.GET_FIRSTJOIN:
                logPM(type, channel, player);
                plugin.sendFirstJoin(player);
                break;
            case SGCraftLinkCommon.GET_JAIL: {
                logPM(type, channel, player);
                CMIModule cmi = plugin.getModule(CMIModule.class);
                if (cmi == null)
                    break;

                CMIModule.JailResult result = cmi.getJailInfo(player);
                if (result == null) {
                    plugin.sendJail(player, "null", 0, 0);
                    break;
                }

                plugin.sendJail(player, result);
                break;
            }
            default: break;
        }
    }

    private void logPM(String type, String channel, Player player) {
        plugin.getLogger().info(channel + ": Received plugin message type " + type + " from " + player.getName());
    }
}
