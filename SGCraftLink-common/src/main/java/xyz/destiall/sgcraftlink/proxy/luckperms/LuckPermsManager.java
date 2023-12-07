package xyz.destiall.sgcraftlink.proxy.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.messenger.message.OutgoingMessage;
import net.luckperms.api.track.Track;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.misc.DataManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LuckPermsManager {
    private final PluginProvider plugin;
    private final LuckPerms api;

    public LuckPermsManager(PluginProvider plugin) {
        this.plugin = plugin;
        api = LuckPermsProvider.get();
        api.getTrackManager().loadAllTracks();
        api.registerMessengerProvider(new ProxyRestMessengerProvider(plugin.getDataManager()));
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

    public PluginProvider getPlugin() {
        return plugin;
    }

    public static class ProxyRestMessenger implements Messenger {
        private final IncomingMessageConsumer consumer;
        private final DataManager module;

        public ProxyRestMessenger(DataManager module, IncomingMessageConsumer consumer) {
            this.consumer = consumer;
            this.module = module;
        }

        public void init() {
            module.getPlugin().getLoggerProvider().warning("Created messenger service");
            module.getMessengers().add(this);
        }

        @Override
        public void close() {
            module.getPlugin().getLoggerProvider().warning("Removing messenger from list");
            module.getMessengers().remove(this);
        }

        @Override
        public void sendOutgoingMessage(@NotNull OutgoingMessage outgoingMessage) {
            String data = outgoingMessage.asEncodedString();
            Map<String, String> body = new HashMap<>();
            body.put("data", data);
            module.sendDataToAll(null, SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE, body, (mappings) -> {});
        }

        public boolean handleIncomingMessage(JSONObject object) {
            String encoded = object.getString("data");
            String server = object.getString("server");
            Map<String, String> body = new HashMap<>();
            body.put("data", encoded);
            body.put("server", server);
            module.sendDataToAll(server, SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE, body, (mappings) -> {});
            boolean accepted = this.consumer.consumeIncomingMessageAsString(encoded);
            if (accepted) {
                module.getPlugin().getLoggerProvider().warning(this.consumer.getClass().getSimpleName() + " accepted incoming message");
            }
            return accepted;
        }
    }

    public static class ProxyRestMessengerProvider implements MessengerProvider {
        private final DataManager dataModule;

        public ProxyRestMessengerProvider(DataManager module) {
            this.dataModule = module;
        }

        @Override
        public @NotNull String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public @NotNull Messenger obtain(@NotNull IncomingMessageConsumer incomingMessageConsumer) {
            ProxyRestMessenger messenger = new ProxyRestMessenger(dataModule, incomingMessageConsumer);
            messenger.init();
            return messenger;
        }
    }

}
