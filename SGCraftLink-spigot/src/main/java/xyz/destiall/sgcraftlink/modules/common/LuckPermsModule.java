package xyz.destiall.sgcraftlink.modules.common;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.messenger.message.OutgoingMessage;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.track.Track;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;

public class LuckPermsModule extends Module implements Listener {
    private final LuckPerms api;

    public LuckPermsModule(SGCraftLinkSpigot plugin, LuckPerms api) {
        super(plugin, null);
        this.api = api;

        api.getTrackManager().loadAllTracks();
        api.registerMessengerProvider(new BackendRestMessengerProvider(plugin.getModule(DataModule.class)));
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        if (!e.getDataType().equals(SGCraftLinkCommon.DATA_GET_RANKS))
            return;

        UUID uuid = UUID.fromString(e.getJson().getString("uuid"));

        User user = getApi().getUserManager().loadUser(uuid).join();
        Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
        Map<Track, Group> tracks = new HashMap<>();
        for (Track track : getApi().getTrackManager().getLoadedTracks()) {
            tracks.put(track, null);
            List<String> groups = track.getGroups();
            for (String group : groups) {
                for (Group inherited : inheritedGroups) {
                    if (inherited.getName().equalsIgnoreCase(group)) {
                        Group current = tracks.get(track);
                        if (current == null || current.getWeight().orElse(0) < inherited.getWeight().orElse(0)) {
                            tracks.put(track, inherited);
                        }
                    }
                }
            }
            if (tracks.get(track) == null) {
                tracks.remove(track);
            }
        }

        List<String> groups = tracks.values().stream().map(Group::getName).collect(Collectors.toList());
        JSONObject res = new JSONObject();
        res.put("ranks", new JSONArray(groups));
        e.setResponse(res);
    }

    public Collection<String> getPermissions(Player sender) {
        try {
            User user = api.getUserManager().loadUser(sender.getUniqueId()).get();
            List<String> perms = user.getDistinctNodes().stream().map(Node::getKey).collect(Collectors.toList());
            Collection<Group> groups = api.getUserManager().loadUser(sender.getUniqueId()).get().getInheritedGroups(user.getQueryOptions());
            for (Group group : groups) {
                perms.addAll(group.getDistinctNodes().stream().map(Node::getKey).collect(Collectors.toList()));
            }
            return perms.stream().distinct().collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sender.getEffectivePermissions().stream()
                .filter(PermissionAttachmentInfo::getValue)
                .map(PermissionAttachmentInfo::getPermission)
                .collect(Collectors.toList());
    }

    public boolean hasPermission(Player player, String permission) {
        return getPermissions(player).contains(permission);
    }

    public LuckPerms getApi() {
        return api;
    }

    public class BackendRestMessenger implements Messenger {
        private final IncomingMessageConsumer consumer;
        private final DataModule module;

        public BackendRestMessenger(DataModule module, IncomingMessageConsumer consumer) {
            this.consumer = consumer;
            this.module = module;
        }

        public void init() {
            module.getPlugin().getLogger().warning("Created messenger service");
            module.getMessengers().add(this);
        }

        @Override
        public void close() {
            module.getPlugin().getLogger().warning("Removing messenger from list");
            module.getMessengers().remove(this);
        }

        @Override
        public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
            String data = outgoingMessage.asEncodedString();
            Map<String, Object> body = createDataMap("data", data);
            plugin.getScheduler().runTask(() -> {
                module.sendProxyData(SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE, body, (s) -> {});
            });
        }

        public boolean handleIncomingMessage(JSONObject object) {
            String encoded = object.getString("data");
            return this.consumer.consumeIncomingMessageAsString(encoded);
        }
    }

    private class BackendRestMessengerProvider implements MessengerProvider {
        private final DataModule dataModule;

        public BackendRestMessengerProvider(DataModule module) {
            this.dataModule = module;
        }

        @Override
        public @NonNull String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            BackendRestMessenger messenger = new BackendRestMessenger(dataModule, incomingMessageConsumer);
            messenger.init();
            return messenger;
        }
    }
}
