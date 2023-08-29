package xyz.destiall.sgcraftlink.modules;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.track.Track;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    public void sendRanks(Player player) {
        if (api == null)
            return;
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            sendRanks(player, user);
            return;
        }
        api.getUserManager().loadUser(player.getUniqueId()).thenAccept(u -> {
            if (u == null) return;
            sendRanks(player, u);
        });
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

    public void sendRanks(Player player, User user) {
        if (api == null)
            return;

        Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
        Map<Track, Group> tracks = new HashMap<>();
        for (Track track : api.getTrackManager().getLoadedTracks()) {
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
            if (tracks.get(track) == null) tracks.remove(track);
        }
        List<String> groups = tracks.values().stream().map(Group::getName).collect(Collectors.toList());
        String ranks = String.join(",", groups);

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF(SGCraftLinkCommon.SET_RANKS);
            out.writeUTF(player.getName());
            out.writeUTF(ranks);
            player.sendPluginMessage(plugin, SGCraftLinkCommon.CHANNEL, stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
