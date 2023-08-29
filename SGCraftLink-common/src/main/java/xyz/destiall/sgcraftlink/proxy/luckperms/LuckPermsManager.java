package xyz.destiall.sgcraftlink.proxy.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.track.Track;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.util.concurrent.CompletableFuture;

public class LuckPermsManager {
    private final PluginProvider plugin;
    private final LuckPerms api;

    public LuckPermsManager(PluginProvider plugin) {
        this.plugin = plugin;
        api = LuckPermsProvider.get();
        api.getTrackManager().loadAllTracks();
    }

    public LuckPerms getApi() {
        return api;
    }

    public CompletableFuture<LuckPermsTrackResult> promote(PlayerProvider player, String trackName) {
        final Track track = api.getTrackManager().getTrack(trackName);
        if (track == null)
            return null;
        return api.getUserManager()
                .loadUser(player.getUUID())
                .thenApply(user -> new LuckPermsTrackResult(user, track.promote(user, ImmutableContextSet.empty()).getGroupTo().orElse("none")));
    }

    public CompletableFuture<LuckPermsTrackResult> demote(PlayerProvider player, String trackName) {
        final Track track = api.getTrackManager().getTrack(trackName);
        if (track == null)
            return null;
        return api.getUserManager()
                .loadUser(player.getUUID())
                .thenApply(user -> new LuckPermsTrackResult(user, track.demote(user, ImmutableContextSet.empty()).getGroupTo().orElse("none")));
    }

}
