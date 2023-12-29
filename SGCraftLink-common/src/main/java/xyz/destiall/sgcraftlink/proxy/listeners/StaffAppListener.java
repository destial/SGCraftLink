package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.LitebansDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StaffAppListener extends ListenerAdapter {
    private final PluginProvider plugin;

    private final Map<String, String> awaitingPromotion;

    public StaffAppListener(PluginProvider plugin) {
        this.plugin = plugin;
        awaitingPromotion = new HashMap<>();
    }

    public Map<String, String> getAwaitingPromotion() {
        return awaitingPromotion;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() && !event.isWebhookMessage())
            return;

        GuildMessageChannel channel = event.getGuildChannel();
        if (!channel.getId().equals("1081249158331113502"))
            return;

        MessageEmbed embed = event.getMessage().getEmbeds().stream().findFirst().orElse(null);
        if (embed == null)
            return;

        MessageEmbed.Field usernameField = embed.getFields().stream()
                .filter(f -> f != null && f.getName() != null && f.getName().startsWith("What is your username, discord and age?"))
                .findFirst().orElse(null);

        if (usernameField == null)
            return;

        String usernameDiscordAge = usernameField.getValue();
        if (usernameDiscordAge == null)
            return;

        String username;
        String discord;
        int age = 0;

        String[] args = usernameDiscordAge.split(", ");
        if (args.length != 3) {
            onError(event, usernameDiscordAge, args);
            return;
        }

        discord = args[1].trim();
        Member user;
        try {
            user = event.getGuild().getMemberByTag(discord);
        } catch (Exception e) {
            onError(event, usernameDiscordAge, args);
            return;
        }

        username = args[0].trim();
        UUID uuid = LitebansDatabase.getUUID(username);
        if (uuid != null) {
            username = LitebansDatabase.getPlayerName(uuid.toString());
        } else {
            username = null;
        }

        if (username == null) {
            channel.sendMessage(args[0].trim() + " tried to apply but has never joined before!").queue();
            onError(event, usernameDiscordAge, args);
            return;
        }

        try {
            age = Integer.parseInt(args[2].trim());
        } catch (Exception e) {
            onError(event, usernameDiscordAge, args);
            return;
        }

        if (age < 16) {
            if (user != null) {
                user.getUser().openPrivateChannel().queue(pm -> pm.sendMessage("You need to be 16 years old or older to apply! Your application has been instantly denied!").queue());
            }
            event.getTextChannel().sendMessage(usernameDiscordAge + " tried to apply but failed age requirements!").queue();
            event.getMessage().delete().queue();
            return;
        }

        String id = user != null ? user.getId() : "null";

        String server = "none";
        MessageEmbed.Field serverField = embed.getFields().stream()
                .filter(f -> f != null && f.getName() != null && f.getName().equalsIgnoreCase("Which servers are you applying to moderate?"))
                .findFirst().orElse(null);
        if (serverField != null && serverField.getValue() != null) {
            server = serverField.getValue().toLowerCase();
        }

        MessageEmbed.Field roleField = embed.getFields().stream()
                .filter(f -> f != null && f.getName() != null && f.getName().equalsIgnoreCase("What rank are you applying for?"))
                .findFirst().orElse(null);

        String role = "none";
        if (roleField != null && roleField.getValue() != null) {
            role = roleField.getValue();
        }

        String suffix = username + "-" + id + "-" + server + "-" + role;

        List<ItemComponent> buttons = new ArrayList<>();
        buttons.add(Button.of(ButtonStyle.SUCCESS, "promote-" + suffix, "Promote"));
        buttons.add(Button.of(ButtonStyle.DANGER, "deny-" + suffix, "Unmute"));
        ActionRow row1 = ActionRow.of(buttons);

        channel.sendMessage("Promote or deny? (" + username + " | " + (user != null ? user.getUser().getName() : "Discord user not found") + ")").setActionRows(row1).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() && !event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId()))
            return;

        String id = event.getButton().getId();
        if (id == null)
            return;

        if (!event.getChannel().getId().equals("1081249158331113502"))
            return;

        String[] args = id.split("-");
        if (args.length != 5)
            return;

        String type = args[0];
        String username = args[1];
        String userId = args[2];
        String server = args[3];
        String role = args[4];
        PrivateChannel pm = null;
        try {
            MiscUtil.parseSnowflake(userId);
            Member member = plugin.getDiscordProvider().getMember(userId);
            if (member != null) {
                pm = member.getUser().openPrivateChannel().complete();
            }
        } catch (Exception ignored) {}

        if (server.equalsIgnoreCase("none") || role.equalsIgnoreCase("none")) {
            event.reply("Server or Role cannot be none! Server: " + server + " | Role: " + role).queue();
            return;
        }

        if (type.equalsIgnoreCase("promote")) {
            String track = "ingame";
            switch (role.toLowerCase()) {
                case "moderator":
                    track = "staff";
                    break;
                case "builder":
                    track = "builder";
                    break;
                default:
                    break;
            }

            if (track.equalsIgnoreCase("ingame")) {
                event.reply("You have to promote this player in game for security purposes!").queue();
                return;
            }
            PlayerProvider player = plugin.getPlayer(username);
            if (player == null) {
                awaitingPromotion.put(username, track);
                event.reply(username + " is not online! Waiting for player to be online to be promoted!").queue();
                return;
            }

            final PrivateChannel finalPm = pm;
            String finalTrack = track;
            event.deferReply().queue(r -> {
                plugin.getLuckPermsManager().promote(player, finalTrack).whenComplete((res, err) -> {
                    if (err != null) {
                        err.printStackTrace();
                        return;
                    }

                    r.editOriginal(username + " has been promoted to " + res.group).queue();
                    if (finalPm != null) {
                        finalPm.sendMessage("Your application has been accepted! You have been promoted to " + res.group).queue();
                    }
                });
            });
        } else if (type.equalsIgnoreCase("deny")) {
            event.reply("Rejected " + username).queue();
            if (pm != null) {
                pm.sendMessage("Your application has been rejected.").queue();
            }
            return;
        }

        event.reply("Unknown type: " + type).queue();
    }

    public void onError(MessageReceivedEvent event, String input, String[] args) {
        String discord = Arrays.stream(args).filter(s -> s.contains("#")).findFirst().orElse(null);
        Member user = null;
        if (discord != null) {
            discord = discord.trim();
            try {
                user = event.getGuild().getMemberByTag(discord);
            } catch (Exception ignored) {}
        }

        if (user == null) {
            String username = input.equals(args[0]) ? input.substring(input.indexOf("#")) : args[0];
            User usr = plugin.getLinkProvider().getUser(username);
            if (usr != null) {
                user = plugin.getDiscordProvider().getMember(usr.getId());
            }
        }

        if (user != null) {
            user.getUser().openPrivateChannel().queue(pm -> pm.sendMessage("You did not properly input your username, discord or age! Your application has been instantly denied!").queue());
        }
        event.getTextChannel().sendMessage(input + " tried to apply but failed form requirements!").queue();
        event.getMessage().delete().queue();
    }
}
