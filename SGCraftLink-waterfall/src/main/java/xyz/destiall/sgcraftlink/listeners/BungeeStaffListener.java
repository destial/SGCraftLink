package xyz.destiall.sgcraftlink.listeners;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeePlayer;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;

public class BungeeStaffListener implements Listener {
    private final SGCraftLinkWaterfall plugin;

    public BungeeStaffListener(SGCraftLinkWaterfall plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(ServerConnectedEvent e) {
        PlayerProvider player = new BungeePlayer(e.getPlayer());
        plugin.getDiscordProvider().getLinkListener().onPlayerJoin(player);
        plugin.getBirthdayManager().onPlayerJoin(player);
        String track = plugin.getDiscordProvider().getStaffAppListener().getAwaitingPromotion().remove(e.getPlayer().getName());
        if (track == null)
            return;

        plugin.getLuckPermsManager().promote(player, track).whenComplete((res, err) -> {
            if (err != null) {
                err.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent e) {
        PlayerProvider player = new BungeePlayer(e.getPlayer());
        plugin.getDiscordProvider().getLinkListener().onPlayerQuit(player);
    }
}
