package xyz.destiall.sgcraftlink.modules.hooks;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ButtonStyle;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Component;
import github.scarsz.discordsrv.dependencies.jda.api.utils.MarkdownUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.messenger.message.OutgoingMessage;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.track.Track;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.items.Pair;
import xyz.destiall.sgcraftlink.items.utils.ButtonCollector;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;
import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class LuckPermsHook extends Module implements Listener {
    private LuckPerms luckperms;
    private TextChannel confirmationChannel;
    private final Map<String, Pair<UUID, Boolean>> inConfirmation = new HashMap<>();
    private final List<String> allowedPlayers = new ArrayList<>();

    public LuckPermsHook(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @Override
    public void initConfig() {
        RegisteredServiceProvider<LuckPerms> provider = plugin
                .getServer()
                .getServicesManager()
                .getRegistration(LuckPerms.class);
        if (provider != null)
            luckperms = provider.getProvider();
        super.initConfig();
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public boolean isEnabled() {
        return luckperms != null;
    }

    @Override
    public void enable() {
        luckperms.getTrackManager().loadAllTracks();
        luckperms.registerMessengerProvider(new BackendRestMessengerProvider(plugin.getModule(DataModule.class)));

        DiscordSRVHook dhook = plugin.getModule(DiscordSRVHook.class);
        if (dhook != null) {
            luckperms.getEventBus().subscribe(NodeAddEvent.class, (event) -> {
                onNodeAdd(event.getNode(), event.getTarget());
            });
        }

        plugin.getScheduler().runTaskTimerAsync(() -> {
            DataModule dataModule = plugin.getModule(DataModule.class);
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission("*") && !allowedPlayers.contains(player.getName())) {
                    CompletableFuture<Boolean> future = new CompletableFuture<>();
                    dataModule.sendProxyData(SGCraftLinkCommon.DATA_GET_WHITELISTED_PERMS, SGCraftLinkCommon.createDataMap("username", player.getName()), (result) -> {
                        if (!result.isSuccess()) {
                            future.complete(false);
                            return;
                        }

                        JSONObject object = result.getJSON();
                        JSONArray array = object.getJSONArray("perms");
                        for (int i = 0; i < array.length(); i++) {
                            if (array.getString(i).equals("*")) {
                                allowedPlayers.add(player.getName());
                                future.complete(true);
                                return;
                            }
                        }
                        future.complete(false);
                    });

                    future.whenComplete((r, e) -> {
                        if (e != null) {
                            e.printStackTrace();
                            player.kickPlayer(color("You have an illegal permission!"));
                            return;
                        }
                        if (r)
                            return;

                        if (confirmationChannel == null) {
                            if (dhook != null) {
                                confirmationChannel = dhook.getTextChannel("1210643175069450251");
                            }
                        }

                        if (confirmationChannel == null)
                            return;

                        EmbedBuilder embed = new EmbedBuilder();
                        final UUID uuid = UUID.randomUUID();
                        embed.setDescription("Unchecked: " + player.getName() + " has the permission `*`!");
                        embed.setTimestamp(Instant.now());
                        embed.setFooter("ID: " + uuid.toString().substring(0, uuid.toString().indexOf("-")) + " | Server: " + plugin.getModule(DataModule.class).getServer());
                        List<Component> components = new ArrayList<>();
                        components.add(Button.of(ButtonStyle.DANGER, "accept-perm-" + uuid, "Yes"));
                        components.add(Button.of(ButtonStyle.SUCCESS, "deny-perm-" + uuid, "No"));
                        components.add(Button.of(ButtonStyle.SUCCESS, "ipban-" + uuid, "IPBan"));
                        confirmationChannel.sendMessageEmbeds(embed.build()).setActionRow(components).queue(m -> {
                            AtomicBoolean responded = new AtomicBoolean(false);
                            ButtonCollector collector = new ButtonCollector(m);
                            collector.setTime(60000L);
                            collector.onCollect(button -> {
                                button.deferEdit().queue();
                                String id = button.getButton().getId();
                                if (id == null)
                                    return;

                                if (id.equalsIgnoreCase("accept-perm-" + uuid)) {
                                    allowedPlayers.add(player.getName());
                                    dataModule.sendProxyData(SGCraftLinkCommon.DATA_SET_WHITELISTED_PERMS, SGCraftLinkCommon.createDataMap("username", player.getName(), "perms", new JSONArray().put("*")), (rr) -> {});
                                    embed.setDescription("Unchecked: " + player.getName() + " has been accepted to get the permission `*`\nBy: " + button.getUser().getAsMention());
                                } else if (id.equalsIgnoreCase("deny-perm-" + uuid)) {
                                    embed.setDescription("Unchecked: " + player.getName() + " has been rejected from getting the permission `*`\nBy: " + button.getUser().getAsMention());
                                    luckperms.getUserManager().modifyUser(player.getUniqueId(), (user) -> user.data().remove(PermissionNode.builder("*").build()));
                                } else if (id.equalsIgnoreCase("ipban-" + uuid)) {
                                    Map<String, Object> cmd = SGCraftLinkCommon.createDataMap("commands", "ipban " + player.getName() + " Attempting to hi-jack server");
                                    Objects.requireNonNull(plugin.getModule(DataModule.class)).sendProxyData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, cmd, (rr) -> {});
                                    embed.setDescription("Unchecked: " + player.getName() + " has been banned `*`\nBy: " + button.getUser().getAsMention());
                                }
                                responded.set(true);
                                collector.stop();
                            });

                            collector.onEnd(l -> {
                                if (!responded.get()) {
                                    embed.setDescription("Unchecked: " + player.getName() + " wanted to have the permission " + MarkdownUtil.monospace("*"));
                                }
                                m.editMessageComponents(Collections.emptyList()).setEmbeds(embed.build()).queue();
                            });

                            collector.start();
                        });
                    });
                }
            }
        }, 20L * 30, 20L * 30L);
    }

    public void onNodeAdd(Node node, PermissionHolder holder) {
        if (Arrays.stream(SGCraftLinkCommon.BLACKLISTED_PERMISSIONS).noneMatch(c -> node.getKey().contains(c))) {
            return;
        }

        if (inConfirmation.containsKey(node.getKey()) && inConfirmation.get(node.getKey()).getValue()) {
            return;
        }

        DataModule dataModule = plugin.getModule(DataModule.class);
        if (holder instanceof User) {
            User user = (User) holder;
            if (dataModule != null) {
                final CompletableFuture<Boolean> future = new CompletableFuture<>();
                dataModule.sendProxyData(SGCraftLinkCommon.DATA_GET_WHITELISTED_PERMS, SGCraftLinkCommon.createDataMap("username", user.getUsername()), (result) -> {
                    if (!result.isSuccess()) {
                        future.complete(false);
                        return;
                    }

                    JSONObject object = result.getJSON();
                    JSONArray array = object.getJSONArray("perms");
                    plugin.getLogger().info(user.getUsername() + " has whitelisted perms " + array);
                    for (int i = 0; i < array.length(); i++) {
                        if (array.getString(i).equals(node.getKey())) {
                            future.complete(true);
                            return;
                        }
                    }

                    future.complete(false);
                });
                try {
                    if (future.get()) {
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }

        holder.data().remove(node);

        if (confirmationChannel == null) {
            DiscordSRVHook dhook = plugin.getModule(DiscordSRVHook.class);
            if (dhook != null) {
                confirmationChannel = dhook.getTextChannel("1210643175069450251");
            }
        }

        if (inConfirmation.containsKey(node.getKey()) || confirmationChannel == null) {
            if (holder instanceof User) {
                luckperms.getUserManager().saveUser((User) holder);
            } else {
                luckperms.getGroupManager().saveGroup((Group) holder);
            }
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        final UUID uuid = UUID.randomUUID();
        embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " wants to have the permission `" + node.getKey() + "`\nDo you accept this?");
        embed.setTimestamp(Instant.now());
        embed.setFooter("ID: " + uuid.toString().substring(0, uuid.toString().indexOf("-")) + " | Server: " + plugin.getModule(DataModule.class).getServer());
        inConfirmation.put(node.getKey(), new Pair<>(uuid, false));
        List<Component> components = new ArrayList<>();
        components.add(Button.of(ButtonStyle.DANGER, "accept-perm-" + uuid, "Yes"));
        components.add(Button.of(ButtonStyle.SUCCESS, "deny-perm-" + uuid, "No"));
        if (holder instanceof User) {
            components.add(Button.of(ButtonStyle.SUCCESS, "ipban-" + uuid, "IPBan"));
        }
        confirmationChannel.sendMessageEmbeds(embed.build()).setActionRow(components).queue(m -> {
            AtomicBoolean responded = new AtomicBoolean(false);
            ButtonCollector collector = new ButtonCollector(m);
            collector.setTime(60000L);
            collector.onCollect(button -> {
                button.deferEdit().queue();
                String id = button.getButton().getId();
                if (id == null)
                    return;

                if (id.equalsIgnoreCase("accept-perm-" + uuid)) {
                    inConfirmation.get(node.getKey()).setValue(true);
                    holder.data().add(node);
                    if (dataModule != null && holder instanceof User) {
                        User user = (User) holder;
                        dataModule.sendProxyData(SGCraftLinkCommon.DATA_SET_WHITELISTED_PERMS, SGCraftLinkCommon.createDataMap("username", user.getUsername(), "perms", new JSONArray().put(node.getKey())), (r) -> {});
                    }
                    embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " has been accepted to get the permission `" + node.getKey() + "`\nBy: " + button.getUser().getAsMention());
                    Action action = api.getActionLogger().actionBuilder()
                            .description("accepted to get " + node.getKey())
                            .timestamp(Instant.now())
                            .sourceName(button.getUser().getName())
                            .targetName(holder.getFriendlyName())
                            .build();
                    api.getActionLogger().broadcastAction(action);
                } else if (id.equalsIgnoreCase("deny-perm-" + uuid)) {
                    embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " has been rejected from getting the permission `" + node.getKey() + "`\nBy: " + button.getUser().getAsMention());
                } else if (id.equalsIgnoreCase("ipban-" + uuid)) {
                    embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " has been banned `" + node.getKey() + "`\nBy: " + button.getUser().getAsMention());
                    Map<String, Object> cmd = SGCraftLinkCommon.createDataMap("commands", "ipban " + holder.getFriendlyName() + " Attempting to hi-jack server");
                    Objects.requireNonNull(plugin.getModule(DataModule.class)).sendProxyData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, cmd, (r) -> {});
                }
                responded.set(true);
                collector.stop();
            });

            collector.onEnd(l -> {
                if (!responded.get()) {
                    embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " wanted to have the permission " + MarkdownUtil.monospace(node.getKey()));
                }
                m.editMessageComponents(Collections.emptyList()).setEmbeds(embed.build()).queue(m2 -> inConfirmation.remove(node.getKey()));
                if (holder instanceof User) {
                    luckperms.getUserManager().saveUser((User) holder);
                } else {
                    luckperms.getGroupManager().saveGroup((Group) holder);
                }
            });

            collector.start();
        });
    }

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        if (!e.getDataType().equals(SGCraftLinkCommon.DATA_GET_RANKS))
            return;

        UUID uuid = UUID.fromString(e.getJson().getString("uuid"));

        User user = getLuckperms().getUserManager().loadUser(uuid).join();
        Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
        Map<Track, Group> tracks = new HashMap<>();
        for (Track track : getLuckperms().getTrackManager().getLoadedTracks()) {
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
            User user;
            if (luckperms.getUserManager().isLoaded(sender.getUniqueId())) {
                user = luckperms.getUserManager().getUser(sender.getUniqueId());
            } else {
                user = luckperms.getUserManager().loadUser(sender.getUniqueId()).get();
            }
            // Must resolve inherited nodes too :(
            // This is the slowest part :(
            return user.resolveDistinctInheritedNodes(user.getQueryOptions()).stream().map(Node::getKey).collect(Collectors.toSet());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sender.getEffectivePermissions().stream()
                .filter(PermissionAttachmentInfo::getValue)
                .map(PermissionAttachmentInfo::getPermission)
                .collect(Collectors.toSet());
    }

    public boolean hasPermission(Player player, String permission) {
        return getPermissions(player).contains(permission);
    }

    public LuckPerms getLuckperms() {
        return luckperms;
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

    @Override
    public boolean isUniversallyCompatible() {
        return true;
    }
}
