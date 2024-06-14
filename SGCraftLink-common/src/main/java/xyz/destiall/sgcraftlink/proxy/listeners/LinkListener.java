package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.util.Tristate;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.ConfigSectionProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.tasks.Task;
import xyz.destiall.sgcraftlink.utils.ButtonCollector;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LinkListener extends ListenerAdapter {
    private final PluginProvider plugin;
    private final EventSubscription<NodeAddEvent> sub;
    private final Map<UUID, Task> authorized;

    public LinkListener(PluginProvider plugin) {
        this.plugin = plugin;
        authorized = new ConcurrentHashMap<>();

        sub = plugin.getLuckPermsManager().getApi().getEventBus().subscribe(NodeAddEvent.class, (event) -> {
            Node node = event.getNode();
            if (!event.isUser())
                return;

            PlayerProvider player = plugin.getPlayer(event.getTarget().getFriendlyName());
            String playerName = player != null ? player.getName() : event.getTarget().getFriendlyName();
            User user = plugin.getLinkProvider().getUser(playerName);
            if (user == null)
                return;

            Member member = plugin.getDiscordProvider().getMember(user.getId());
            if (member == null)
                return;

            if (node.isNegated())
                return;

            if (!node.getKey().startsWith("suffix"))
                return;

            String[] args = node.getKey().split("//.");
            String suffix = ColorProvider.ChatColor.strip(args[args.length - 1]);

            plugin.getScheduler().schedule(() -> plugin.getDiscordProvider().updateNickname(member, playerName, suffix), 1L, TimeUnit.SECONDS);
        });
    }

    public EventSubscription<NodeAddEvent> getSub() {
        return sub;
    }

    public void onPlayerQuit(PlayerProvider player) {
        if (!authorized.containsKey(player.getUUID())) {
            return;
        }

        Task task = authorized.get(player.getUUID());
        task.cancel();
        authorized.put(player.getUUID(), plugin.getScheduler().schedule(() -> authorized.remove(player.getUUID()), 10L, TimeUnit.MINUTES));
    }

    public Collection<UUID> getAuthorized() {
        return authorized.keySet();
    }

    public void setAuthorized(PlayerProvider player) {
        authorized.put(player.getUUID(), plugin.getScheduler().schedule(() -> {
            if (!player.isOnline()) {
                authorized.remove(player.getUUID());
            } else {
                setAuthorized(player);
            }
        }, 10L, TimeUnit.MINUTES));
    }

    public void authorize(PlayerProvider player) {
        String id = plugin.getLinkProvider().getId(player.getName());
        if (id == null)
            return;

        Member member = plugin.getDiscordProvider().getMember(id);
        if (member == null)
            return;

        plugin.getScheduler().schedule(() -> {
            if (player.isOnline() && player.hasPermission("sgc.report.bypass")) {
                ConfigSectionProvider<?> data = plugin.getLinkProvider().getData(player.getName());
                if (data != null && data.contains("ip")) {
                    String oldIp = data.getString("ip");
                    if (player.getIP().equals(oldIp)) {
                        setAuthorized(player);
                        return;
                    }
                }

                plugin.getLoggerProvider().info("Asking " + player.getName() + " for authorization!");
                member.getUser().openPrivateChannel().queue(pm -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    if (authorized.containsKey(player.getUUID())) {
                        embed.setColor(Color.GREEN);
                        embed.setDescription("Continuing minecraft session!\nIGN: " + player.getName() + "\nIP: ||" + player.getIP() + "||");
                        embed.setTimestamp(Instant.now());
                        pm.sendMessageEmbeds(embed.build()).queue();
                        player.sendMessage(ColorProvider.of("&aContinuing authorized minecraft session!"));
                        return;
                    }
                    embed.setColor(Color.RED);
                    embed.setDescription("Your minecraft account is online! Is this you?\nIGN: " + player.getName() + "\nIP: ||" + player.getIP() + "||");
                    embed.setTimestamp(Instant.now());
                    ActionRow row = ActionRow.of(Button.of(ButtonStyle.PRIMARY, "yes-" + player.getName(), "Yes"), Button.of(ButtonStyle.DANGER, "no-" + player.getName(), "No"));
                    player.sendMessage(ColorProvider.of("&c&lCheck your discord direct messages for authorization!"));
                    pm.sendMessageEmbeds(embed.build()).setActionRows(row).onErrorMap((ex) -> null).queue(m -> {
                        if (m == null) {
                            player.sendMessage(ColorProvider.of("&c&lUnable to DM you on discord!"));
                            return;
                        }
                        ButtonCollector collector = new ButtonCollector(m);
                        collector.setLimit(1);
                        collector.setTime(60000L);

                        collector.onCollect(button -> {
                            if (button.getButton().getId().startsWith("yes")) {
                                embed.setDescription("You authorized your login from this IP!\nIGN: " + player.getName() + "\nIP: ||" + player.getIP() + "||");
                                embed.setColor(Color.GREEN);
                                setAuthorized(player);
                                if (data != null) {
                                    data.set("ip", player.getIP());
                                    plugin.getLinkProvider().updateUser(player.getName(), data);
                                }
                            } else {
                                embed.setDescription("You unauthorized your login!nIGN: " + player.getName() + "\nIP: ||" + player.getIP() + "||");
                                authorized.remove(player.getUUID());
                            }
                        });

                        collector.onEnd(c -> {
                            m.editMessageEmbeds(embed.build()).setActionRows(Collections.emptyList()).queue();
                        });

                        collector.start();
                    });

                });
            }
        }, 3L, TimeUnit.SECONDS);
    }

    public void onPlayerJoin(PlayerProvider player) {
        String id = plugin.getLinkProvider().getId(player.getName());
        if (id == null) {
            plugin.getLuckPermsManager().getApi().getUserManager().modifyUser(player.getUUID(), u -> {
                InheritanceNode linked = InheritanceNode.builder("dclinked").build();
                if (u.data().contains(linked, NodeEqualityPredicate.ONLY_KEY) == Tristate.TRUE) {
                    u.data().remove(linked);
                    plugin.getLoggerProvider().info(player.getName() + " is unlinking! Removing rank...");
                }
            });
            return;
        }

        Member member = plugin.getDiscordProvider().getMember(id);
        if (member == null)
            return;

        authorize(player);

        plugin.getLuckPermsManager().getApi().getUserManager().modifyUser(player.getUUID(), u -> {
            InheritanceNode node = InheritanceNode.builder("booster").build();
            InheritanceNode linked = InheritanceNode.builder("dclinked").build();

            if (u.data().contains(linked, NodeEqualityPredicate.ONLY_KEY) == Tristate.FALSE) {
                u.data().add(linked);
                plugin.getLoggerProvider().info(player.getName() + " is a linked member! Adding rank...");
            }

            if (member.isBoosting()) {
                u.data().add(node);
                plugin.getLoggerProvider().info(player.getName() + " is a Nitro Booster! Adding rank...");
            } else {
                u.data().remove(node);
            }
        });

        plugin.getDiscordProvider().updateNickname(member, player);
        plugin.getDiscordProvider().updateRankStatus(player, member);
    }

    @Override
    public void onGuildMemberUpdateBoostTime(@NotNull GuildMemberUpdateBoostTimeEvent event) {
        Member member = event.getMember();
        if (member.getUser().isBot() || member.getId().equals(plugin.getDiscordProvider().getJda().getSelfUser().getId()))
            return;

        String name = plugin.getLinkProvider().getUsername(member.getUser());
        if (name == null)
            return;

        PlayerProvider player = plugin.getPlayer(name);
        if (player == null)
            return;

        if (event.getNewTimeBoosted() == null) {
            plugin.getLuckPermsManager().getApi().getUserManager().modifyUser(player.getUUID(), u -> {
                Node node = InheritanceNode.builder("booster").build();
                u.data().remove(node);
                plugin.getLoggerProvider().info(player.getName() + " stopped boosting! Removing rank...");
            });
        } else if (event.getOldTimeBoosted() == null) {
            plugin.getLuckPermsManager().getApi().getUserManager().modifyUser(player.getUUID(), u -> {
                Node node = InheritanceNode.builder("booster").build();
                u.data().add(node);
                plugin.getLoggerProvider().info(player.getName() + " just boosted! Adding rank...");
            });
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            User user = event.getAuthor();
            if (user.isBot() || user.getId().equals(plugin.getDiscordProvider().getJda().getSelfUser().getId())) return;

            Message message = event.getMessage();
            String content = message.getContentRaw().trim();

            String name = plugin.getLinkProvider().getUsername(user);
            if (name != null) {
                message.reply("You are already linked to " + name).queue();
                return;
            }
            try {
                int code = Integer.parseInt(content);
                name = plugin.getLinkProvider().getName(code);
                if (name == null) {
                    message.reply("That is not a valid code!").queue();
                    return;
                }
                if (plugin.getLinkProvider().link(name, user)) {
                    message.reply("You have linked your account to " + name).queue();
                    plugin.getLinkProvider().invalidate(code);
                } else {
                    message.reply("Player " + name + " is not online!").queue();
                }
            } catch (Exception e) {
                message.reply("That is not a valid code!").queue();
            }
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        if (member.getUser().isBot() || member.getId().equals(plugin.getDiscordProvider().getJda().getSelfUser().getId())) return;
        String name = plugin.getLinkProvider().getUsername(member.getUser());
        if (name == null)
            return;

        PlayerProvider player = plugin.getPlayer(name);
        if (player != null) {
            String suffix = player.getSuffix();
            if (suffix != null) {
                name += " " + suffix;
            }
        }
        try {
            member.modifyNickname(name).delay(5, TimeUnit.SECONDS).queue(q -> {
            }, err -> {
            });
        } catch (Exception ignored) {}
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        plugin.getLinkProvider().unlinkId(event.getUser().getId());
    }
}
