package xyz.destiall.sgcraftlink.addons;

import ch.andre601.advancedserverlist.api.PlaceholderProvider;
import ch.andre601.advancedserverlist.api.objects.GenericPlayer;
import ch.andre601.advancedserverlist.api.objects.GenericServer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;

public class VelocityASLPlaceholder extends PlaceholderProvider {
    private String highest;
    private String lowest;
    public VelocityASLPlaceholder(SGCraftLinkVelocity plugin) {
        super("sgclink");
        plugin.getLoggerProvider().info(
                "name:" + plugin.getProxy().getVersion().getName() +
                     ",version:" + plugin.getProxy().getVersion().getVersion() +
                        ",vendor:" + plugin.getProxy().getVersion().getVersion()
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

    @Subscribe
    public void onFirstPing(ProxyPingEvent e) {
        if (highest == null) {
            try {
                String str = e.getPing().getVersion().getName();
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
