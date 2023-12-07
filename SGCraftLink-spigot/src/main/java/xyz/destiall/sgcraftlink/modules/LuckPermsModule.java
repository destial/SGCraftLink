package xyz.destiall.sgcraftlink.modules;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.messenger.message.OutgoingMessage;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LuckPermsModule extends Module {
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

    public static class BackendRestMessenger implements Messenger {
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
            Map<String, String> body = new HashMap<>();
            body.put("data", data);
            module.sendProxyData(SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE, body, (s, c) -> {});
        }

        public boolean handleIncomingMessage(JSONObject object) {
            String encoded = object.getString("data");
            boolean accepted = this.consumer.consumeIncomingMessageAsString(encoded);
            if (accepted) {
                module.getPlugin().getLogger().warning(this.consumer.getClass().getSimpleName() + " accepted incoming message");
            }
            return accepted;
        }
    }

    private static class BackendRestMessengerProvider implements MessengerProvider {
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
