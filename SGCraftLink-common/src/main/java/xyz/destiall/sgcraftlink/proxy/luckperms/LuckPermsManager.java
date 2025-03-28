package xyz.destiall.sgcraftlink.proxy.luckperms;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.messenger.message.OutgoingMessage;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.track.Track;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ConfigSectionProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.misc.DataManager;
import xyz.destiall.sgcraftlink.utils.ButtonCollector;
import xyz.destiall.sgcraftlink.utils.Pair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;

public class LuckPermsManager {
    private final PluginProvider plugin;
    private final LuckPerms api;
    private TextChannel confirmationChannel = null;
    private final Map<String, Pair<UUID, Boolean>> inConfirmation = new HashMap<>();

    public LuckPermsManager(PluginProvider plugin) {
        this.plugin = plugin;
        api = LuckPermsProvider.get();
        api.getTrackManager().loadAllTracks();
        api.registerMessengerProvider(new ProxyRestMessengerProvider(plugin.getDataManager()));

        api.getEventBus().subscribe(NodeAddEvent.class, (event) -> {
            onNodeAdd(Collections.singletonList(event.getNode()), event.getTarget());
        });
    }

    public void onNodeAdd(Collection<Node> nodes, PermissionHolder holder) {
        for (Node node : nodes) {
            if (Arrays.stream(SGCraftLinkCommon.BLACKLISTED_PERMISSIONS).noneMatch(c -> node.getKey().contains(c))) {
                continue;
            }

            if (inConfirmation.containsKey(node.getKey()) && inConfirmation.get(node.getKey()).getValue()) {
                continue;
            }

            if (holder instanceof User) {
                User user = (User) holder;
                ConfigSectionProvider<?> data = plugin.getLinkProvider().getData(user.getUsername());
                if (data != null && data.contains("perms")) {
                    List<String> perms = data.getStringList("perms");
                    if (perms.contains(node.getKey())) {
                        continue;
                    }
                }
            }

            holder.data().remove(node);
            if (holder instanceof User) {
                api.getMessagingService().ifPresent(s -> s.pushUserUpdate((User) holder));
            }

            if (confirmationChannel == null) {
                confirmationChannel = plugin.getDiscordProvider().getJda().getTextChannelById("1210643175069450251");
            }

            if (inConfirmation.containsKey(node.getKey()) || confirmationChannel == null) {
                if (holder instanceof User) {
                    api.getUserManager().saveUser((User) holder);
                    api.getMessagingService().ifPresent(s -> s.pushUserUpdate((User) holder));
                } else if (holder instanceof Group) {
                    api.getGroupManager().saveGroup((Group) holder);
                }
                continue;
            }

            EmbedBuilder embed = new EmbedBuilder();
            final UUID uuid = UUID.randomUUID();
            embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " wants to have the permission " + MarkdownUtil.monospace(node.getKey()) + "\nDo you accept this?");
            embed.setTimestamp(Instant.now());
            embed.setFooter("ID: " + uuid.toString().substring(0, uuid.toString().indexOf("-")) + " | Server: Proxy");
            inConfirmation.put(node.getKey(), new Pair<>(uuid, false));
            List<ItemComponent> components = new ArrayList<>();
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
                        if (holder instanceof User) {
                            User user = (User) holder;
                            ConfigSectionProvider<?> data = plugin.getLinkProvider().getData(user.getUsername());
                            if (data != null) {
                                List<String> perms = new ArrayList<>();
                                if (data.contains("perms")) {
                                    perms.addAll(data.getStringList("perms"));
                                }
                                perms.add(node.getKey());
                                data.set("perms", perms);
                                plugin.getLinkProvider().updateUser(user.getUsername(), data);
                            }
                        }
                        Action action = api.getActionLogger().actionBuilder()
                                .description("accepted to get " + node.getKey())
                                .timestamp(Instant.now())
                                .sourceName(button.getUser().getName())
                                .targetName(holder.getFriendlyName())
                                .build();
                        api.getActionLogger().broadcastAction(action);
                        embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " has been accepted to get the permission " + MarkdownUtil.monospace(node.getKey()) + "\nBy: " + button.getUser().getAsMention());
                    } else if (id.equalsIgnoreCase("deny-perm-" + uuid)) {
                        embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " has been rejected from getting the permission " + MarkdownUtil.monospace(node.getKey()) + "\nBy: " + button.getUser().getAsMention());
                    } else if (id.equalsIgnoreCase("ipban-" + uuid)) {
                        embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " has been banned `" + node.getKey() + "`\nBy: " + button.getUser().getAsMention());
                        plugin.dispatchCommand("ipban " + holder.getFriendlyName() + " Attempting to hi-jack server");
                    }
                    responded.set(true);
                    collector.stop();
                });

                collector.onEnd(l -> {
                    if (!responded.get()) {
                        embed.setDescription(holder.getClass().getSimpleName() + ": " + holder.getFriendlyName() + " wanted to have the permission " + MarkdownUtil.monospace(node.getKey()));
                    }
                    m.editMessageComponents(Collections.emptyList()).setEmbeds(embed.build()).queue(mm -> inConfirmation.remove(node.getKey()));
                    if (holder instanceof User) {
                        api.getUserManager().saveUser((User) holder);
                        api.getMessagingService().ifPresent(s -> s.pushUserUpdate((User) holder));
                    } else if (holder instanceof Group) {
                        api.getGroupManager().saveGroup((Group) holder);
                    }
                });

                collector.start();
            });
        }
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
            Map<String, Object> body = createDataMap("data", encoded, "server", server);
            module.sendDataToAll(server, SGCraftLinkCommon.DATA_LUCKPERMS_SERVICE, body, (mappings) -> {});
            return this.consumer.consumeIncomingMessageAsString(encoded);
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
