package xyz.destiall.sgcraftlink.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityPlayer;

public class VelocityStaffListener {
    private final SGCraftLinkVelocity plugin;

    public VelocityStaffListener(SGCraftLinkVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerConnect(PostLoginEvent e) {
        PlayerProvider player = new VelocityPlayer(e.getPlayer());
        plugin.getDiscordProvider().getLinkListener().onPlayerJoin(player);
        plugin.getBirthdayManager().onPlayerJoin(player);

        String track = plugin.getDiscordProvider().getStaffAppListener().getAwaitingPromotion().remove(e.getPlayer().getUsername());
        if (track == null)
            return;

        plugin.getLuckPermsManager().promote(player, track).whenComplete((res, err) -> {
            if (err != null) {
                err.printStackTrace();
            }
        });
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent e) {
        PlayerProvider player = new VelocityPlayer(e.getPlayer());
        plugin.getDiscordProvider().getLinkListener().onPlayerQuit(player);
    }
}
