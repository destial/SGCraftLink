package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.util.Tristate;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.util.concurrent.TimeUnit;

public class LinkListener extends ListenerAdapter {
    private final PluginProvider plugin;
    private final EventSubscription<NodeAddEvent> sub;

    public LinkListener(PluginProvider plugin) {
        this.plugin = plugin;

        sub = plugin.getLuckPermsManager().getApi().getEventBus().subscribe(NodeAddEvent.class, (event) -> {
            Node node = event.getNode();
            if (!event.isUser())
                return;

            PlayerProvider player = plugin.getPlayer(event.getTarget().getFriendlyName());
            User user = plugin.getLinkProvider().getUser(player.getName());
            if (user == null)
                return;

            Member member = plugin.getDiscordProvider().getMember(user.getId());
            if (member == null)
                return;

            if (node.isNegated())
                return;

            if (!node.getKey().startsWith("suffix"))
                return;

            plugin.getScheduler().schedule(() -> plugin.getDiscordProvider().updateNickname(player, member), 1L, TimeUnit.SECONDS);
        });
    }

    public EventSubscription<NodeAddEvent> getSub() {
        return sub;
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

        plugin.getDiscordProvider().updateNickname(player, member);
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
