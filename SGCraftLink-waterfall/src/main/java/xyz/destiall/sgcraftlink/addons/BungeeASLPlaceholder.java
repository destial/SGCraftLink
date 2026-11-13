package xyz.destiall.sgcraftlink.addons;

import ch.andre601.advancedserverlist.api.PlaceholderProvider;
import ch.andre601.advancedserverlist.api.objects.GenericPlayer;
import ch.andre601.advancedserverlist.api.objects.GenericServer;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;

public class BungeeASLPlaceholder extends PlaceholderProvider implements Listener {
    private String highest;
    private String lowest;

    public BungeeASLPlaceholder(SGCraftLinkWaterfall plugin) {
        super("sgclink");
        plugin.getLoggerProvider().info(
                "name:" + plugin.getProxy().getName() +
                        ",version:" + plugin.getProxy().getVersion()
        );
        highest = null;
        lowest = null;
    }

    @Override
    public @Nullable String parsePlaceholder(@NotNull String placeholder, @NotNull GenericPlayer player, @NotNull GenericServer server) {
        if (placeholder.equalsIgnoreCase("mxver")) {
            return highest == null ? "Latest" : highest;
        } else if (placeholder.equalsIgnoreCase("mnver")) {
            return lowest == null ? "1.8" : lowest;
        } else if (placeholder.equalsIgnoreCase("rgver")) {
            if (highest == null) {
                return "Latest";
            }

            return lowest + "-" + highest;
        }
        return "";
    }

    @EventHandler
    public void onFirstPing(ProxyPingEvent e) {
        if (highest == null) {
            try {
                String str = e.getResponse().getVersion().getName();
                String[] split = str.split(" ");
                String range = split[split.length - 1];
                String[] range_split = range.split("-");
                lowest = range_split[0];
                highest = range_split[1];
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
}
