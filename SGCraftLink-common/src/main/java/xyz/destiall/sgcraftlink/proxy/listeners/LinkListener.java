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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LinkListener extends ListenerAdapter {
    private final PluginProvider plugin;
    private final EventSubscription<NodeAddEvent> sub;
    private final Map<UUID, Task> authorizedCache;
    private final Map<UUID, Task> repeatingTasks;

    public LinkListener(PluginProvider plugin) {
        this.plugin = plugin;
        authorizedCache = new ConcurrentHashMap<>();
        repeatingTasks = new ConcurrentHashMap<>();

        sub = plugin.getLuckPermsManager().getApi().getEventBus().subscribe(NodeAddEvent.class, (event) -> {
            Node node = event.getNode();
            if (!event.isUser())
                return;

            PlayerProvider player = plugin.getPlayer(event.getTarget().getFriendlyName());
            String playerName = player.isOnline() ? player.getName() : event.getTarget().getFriendlyName();
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

            plugin.getScheduler().schedule(() -> {
                String name = playerName;
                if (player.isOnline()) {
                    String suffix = player.getSuffix();
                    if (suffix != null) {
                        name += " " + suffix;
                    }
                }
                try {
                    member.modifyNickname(name).delay(3, TimeUnit.SECONDS).queue(q -> {}, err -> {});
                } catch (Exception ignored) {}
            }, 1L, TimeUnit.SECONDS);
        });
    }

    public EventSubscription<NodeAddEvent> getSub() {
        return sub;
    }

    public void onPlayerQuit(PlayerProvider player) {
        cancelRepeatMessage(player);

        if (!authorizedCache.containsKey(player.getUUID())) {
            return;
        }

        Task task = authorizedCache.get(player.getUUID());
        task.cancel();
        authorizedCache.put(player.getUUID(), plugin.getScheduler().schedule(() -> authorizedCache.remove(player.getUUID()), 10L, TimeUnit.MINUTES));
    }

    public Collection<UUID> getAuthorizedCache() {
        return authorizedCache.keySet();
    }

    public void setAuthorizedCache(PlayerProvider player) {
        if (!player.isOnline())
            return;

        authorizedCache.put(player.getUUID(), plugin.getScheduler().schedule(() -> {
            if (!player.isOnline()) {
                authorizedCache.remove(player.getUUID());
            } else {
                setAuthorizedCache(player);
            }
        }, 10L, TimeUnit.MINUTES));
    }

    public void cancelRepeatMessage(PlayerProvider player) {
        if (!player.isOnline())
            return;

        Task r = repeatingTasks.remove(player.getUUID());
        if (r != null) {
            r.cancel();
        }
    }

    public boolean authorize(PlayerProvider player) {
        if (!player.isOnline())
            return false;
        boolean isStaff = player.hasPermission("sgc.report.bypass");

        String id = plugin.getLinkProvider().getId(player.getName());
        if (id == null) {
            if (isStaff) {
                Task repeatingTask = plugin.getScheduler().schedule(() -> {
                    player.sendMessage(ColorProvider.of("&c! STAFF ALERT: You have to link your discord account to your minecraft account !"));
                    player.sendMessage(ColorProvider.of("&c! Type '/discord link' in-game or '/mclink' in the discord server !"));
                    player.sendMessage(ColorProvider.of("&c! You have 2 minutes to complete this before getting kicked !"));
                }, 1L, 8L, TimeUnit.SECONDS);
                repeatingTasks.put(player.getUUID(), repeatingTask);

                plugin.getScheduler().schedule(() -> {
                    if (authorizedCache.containsKey(player.getUUID())) {
                        repeatingTasks.remove(player.getUUID());
                        repeatingTask.cancel();
                        return;
                    }
                    player.kick("Timeout exceeded! Unauthorized login!");
                }, 2L, TimeUnit.MINUTES);
            }
            return false;
        }

        Member member = plugin.getDiscordProvider().getMember(id);

        plugin.getScheduler().schedule(() -> {
            if (player.isOnline() && isStaff) {
                ConfigSectionProvider<?> data = plugin.getLinkProvider().getData(player.getName());
                if (data != null && data.contains("ip")) {
                    String oldIp = data.getString("ip");
                    if (player.getIP().equals(oldIp)) {
                        setAuthorizedCache(player);
                        return;
                    }
                }

                plugin.getLoggerProvider().info("Asking " + player.getName() + " for authorization!");
                member.getUser().openPrivateChannel().queue(pm -> {
                    EmbedBuilder embed = new EmbedBuilder();
                    if (authorizedCache.containsKey(player.getUUID())) {
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
                    ActionRow row = ActionRow.of(
                            Button.of(ButtonStyle.PRIMARY, "yes-" + player.getName(), "Yes"),
                            Button.of(ButtonStyle.DANGER, "no-" + player.getName(), "No"),
                            Button.of(ButtonStyle.UNKNOWN, "ipban-" + player.getName(), "IP Ban")
                    );
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
                            String buttonId = button.getButton().getId();
                            if (buttonId.startsWith("yes")) {
                                embed.setDescription("You authorized your login from this IP!\nIGN: " + player.getName() + "\nIP: ||" + player.getIP() + "||");
                                embed.setColor(Color.GREEN);
                                setAuthorizedCache(player);
                                if (data != null) {
                                    data.set("ip", player.getIP());
                                    plugin.getLinkProvider().updateUser(player.getName(), data);
                                }
                            } else if (buttonId.startsWith("no")) {
                                embed.setDescription("You unauthorized your login!nIGN: " + player.getName() + "\nIP: ||" + player.getIP() + "||");
                                player.kick("Unauthorized login");
                                authorizedCache.remove(player.getUUID());
                            } else {
                                embed.setDescription("Banning IP: " + player.getIP());
                                plugin.dispatchCommand("ipban " + player.getIP() + " -I Attempting to compromise account!");
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

        return true;
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
        }

        if (!authorize(player))
            return;

        Member member = plugin.getDiscordProvider().getMember(id);
        if (member == null)
            return;

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

        if (!member.getRoles().contains(plugin.getDiscordProvider().getLinkedRole())) {
            member.getGuild().addRoleToMember(member, plugin.getDiscordProvider().getLinkedRole()).queue();
        }

        plugin.getDiscordProvider().updateNickname(member, player);
        plugin.getDiscordProvider().updateRankStatus(player, member);
    }

    @Override
    public void onGuildMemberUpdateBoostTime(@NotNull GuildMemberUpdateBoostTimeEvent event) {
        Member member = event.getMember();
        if (member.getUser().isBot() || member.getId().equals(plugin.getDiscordProvider().getJda().getSelfUser().getId()))
            return;

        List<String> names = plugin.getLinkProvider().getUsernames(member.getUser());
        if (names.isEmpty())
            return;

        for (String name : names) {
            PlayerProvider player = plugin.getPlayer(name);
            if (!player.isOnline())
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
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            User user = event.getAuthor();
            if (user.isBot() || user.getId().equals(plugin.getDiscordProvider().getJda().getSelfUser().getId())) return;

            Message message = event.getMessage();
            String content = message.getContentRaw().trim();

            int code;
            try {
                code = Integer.parseInt(content);
            } catch (NumberFormatException e) {
                message.reply("That is not a valid code! It is 6 digits!").queue();
                return;
            }

            String name = plugin.getLinkProvider().getName(code);
            if (name == null) {
                message.reply("That is not a valid code!").queue();
                return;
            }

            List<String> names = plugin.getLinkProvider().getUsernames(user);
            if (names.contains(name)) {
                message.reply("You are already linked to " + name).queue();
                return;
            }


            if (!plugin.getLinkProvider().link(name, user)) {
                message.reply("Player " + name + " is not online!").queue();
                return;
            }

            message.reply("You have linked your account to " + name).queue();
            plugin.getLinkProvider().invalidate(code);
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        if (member.getUser().isBot() || member.getId().equals(plugin.getDiscordProvider().getJda().getSelfUser().getId())) return;
        List<String> names = plugin.getLinkProvider().getUsernames(member.getUser());
        if (names.isEmpty())
            return;

        String first = names.get(0);
        PlayerProvider player = plugin.getPlayer(first);
        if (player.isOnline()) {
            String suffix = player.getSuffix();
            if (suffix != null) {
                first += " " + suffix;
            }
        }
        try {
            member.modifyNickname(first).delay(5, TimeUnit.SECONDS).queue(q -> {}, err -> {});
        } catch (Exception ignored) {}
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        plugin.getLinkProvider().unlinkAllId(event.getUser().getId());
    }
}
