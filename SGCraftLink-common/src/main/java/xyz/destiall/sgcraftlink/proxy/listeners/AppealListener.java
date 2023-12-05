package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.LitebansDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AppealListener extends ListenerAdapter {
    private final PluginProvider plugin;

    public AppealListener(PluginProvider plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() && !event.isWebhookMessage())
            return;

        GuildMessageChannel channel = event.getGuildChannel();
        if (!channel.getId().equals("1082233423537639475"))
            return;

        MessageEmbed embed = event.getMessage().getEmbeds().stream().findFirst().orElse(null);
        if (embed == null)
            return;

        MessageEmbed.Field usernameField = embed.getFields().stream()
                .filter(f -> f != null && f.getName() != null && f.getName().equalsIgnoreCase("username"))
                .findFirst().orElse(null);

        if (usernameField == null)
            return;

        String username = usernameField.getValue();
        if (username == null)
            return;

        UUID uuid = LitebansDatabase.getUUID(username);
        if (uuid == null) {
            channel.sendMessage(username + " has never joined yet!").queue();
            return;
        }
        username = LitebansDatabase.getPlayerName(uuid.toString());

        MessageEmbed.Field discordField = embed.getFields().stream()
                .filter(f -> f != null && f.getName() != null && f.getName().equalsIgnoreCase("Provide your discord username"))
                .findFirst().orElse(null);

        Member user = null;
        if (discordField != null && discordField.getValue() != null) {
            String discord = discordField.getValue();
            try {
                user = event.getGuild().getMemberByTag(discord);
            } catch (Exception ignored) {
                List<Member> members = event.getGuild().getMembersByName(discord, true);
                if (members.size() == 1) {
                    user = members.get(0);
                }
            }
        }
        if (user == null) {
            String linkId = plugin.getLinkProvider().getId(username);
            if (linkId != null) {
                user = plugin.getDiscordProvider().getMember(linkId);
            }
        }
        String id = user != null ? user.getId() : "null";

        String server = "none";
        MessageEmbed.Field serverField = embed.getFields().stream()
                .filter(f -> f != null && f.getName() != null && f.getName().equalsIgnoreCase("Which server were you banned from?"))
                .findFirst().orElse(null);
        if (serverField != null && serverField.getValue() != null) {
            server = serverField.getValue().toLowerCase();
        }

        String appealReason = "";
        MessageEmbed.Field reasonField = embed.getFields().stream()
                .filter(f -> f != null && f.getName() != null && f.getName().equalsIgnoreCase("Why were you punished?"))
                .findFirst().orElse(null);
        if ( reasonField != null &&  reasonField.getValue() != null) {
            appealReason =  reasonField.getValue().toLowerCase();
        }

        String args = username + "-" + id + "-" + server;

        List<ItemComponent> buttons = new ArrayList<>();
        buttons.add(new ButtonImpl("unban-" + args, "Unban", ButtonStyle.PRIMARY, false, null));
        buttons.add(new ButtonImpl("unmute-" + args, "Unmute", ButtonStyle.PRIMARY, false, null));

        if (appealReason.contains("xray") || appealReason.contains("x-ray") || appealReason.contains("exploit")) {
            buttons.add(new ButtonImpl("jail3h-" + args, "Jail 3h", ButtonStyle.PRIMARY, false, null));
            buttons.add(new ButtonImpl("jail8h-" + args, "Jail 8h", ButtonStyle.PRIMARY, false, null));
            buttons.add(new ButtonImpl("jail12-" + args, "Jail 12h", ButtonStyle.PRIMARY, false, null));
        }
        ActionRow row1 = ActionRow.of(buttons);
        buttons.clear();

        buttons.add(new ButtonImpl("tempban7d-" + args, "Tempban 7d", ButtonStyle.PRIMARY, false, null));
        buttons.add(new ButtonImpl("tempban5d-" + args, "Tempban 5d", ButtonStyle.PRIMARY, false, null));
        buttons.add(new ButtonImpl("tempban3d-" + args, "Tempban 3d", ButtonStyle.PRIMARY, false, null));
        buttons.add(new ButtonImpl("deny-" + args, "Deny", ButtonStyle.DANGER, false, null));

        ActionRow row2 = ActionRow.of(buttons);
        channel.sendMessage("Accept or deny? (" + username + " | " + (user != null ? user.getUser().getAsTag() : "Discord user not found") + ")").setActionRows(row1, row2).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild() && !event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            super.onButtonInteraction(event);
            return;
        }

        String id = event.getButton().getId();
        if (id == null) {
            super.onButtonInteraction(event);
            return;
        }

        if (!event.getChannel().getId().equals("1082233423537639475")) {
            super.onButtonInteraction(event);
            return;
        }

        String[] args = id.split("-");

        if (args.length != 4) {
            super.onButtonInteraction(event);
            return;
        }

        String username = args[1];
        String userId = args[2];
        String server = args[3];
        PrivateChannel pm = null;
        try {
            MiscUtil.parseSnowflake(userId);
            Member member = plugin.getDiscordProvider().getMember(userId);
            if (member != null) {
                pm = member.getUser().openPrivateChannel().complete();
            }
        } catch (Exception ignored) {}

        String executor = plugin.getLinkProvider().getUsername(event.getUser());
        if (executor == null) {
            executor = event.getUser().getName();
        }

        switch (args[0]) {
            case "unban":
                plugin.dispatchCommand("unban " + username + " --sender=" + executor + " Appeal Accepted");
                if (pm != null) {
                    pm.sendMessage("Your appeal has been accepted. You have been unbanned by " + executor).queue();
                }
                break;
            case "unmute":
                plugin.dispatchCommand("unmute " + username + " --sender=" + executor + " Appeal Accepted");
                if (pm != null) {
                    pm.sendMessage("Your appeal has been accepted. You have been unmuted by " + executor).queue();
                }
                break;
            case "deny":
                if (pm != null) {
                    pm.sendMessage("Your appeal has been rejected by " + executor).queue();
                }
                break;
            default: {
                if (args[0].startsWith("tempban")) {
                    String period = args[0].replace("tempban", "").trim();
                    plugin.dispatchCommand("tempban " + username + " --sender=" + executor + " " + period + " Appeal Accepted (" + period + " Tempban)");
                    if (pm != null) {
                        pm.sendMessage("Your appeal has been accepted. You have been temporarily banned for " + period.replace("d", "") + " days by " + executor).queue();
                    }
                } else if (args[0].startsWith("jail")) {
                    String period = args[0].replace("jail", "").trim();
                    int hours = Integer.parseInt(period.replace("h", ""));
                    plugin.dispatchCommand("unban " + username + " --sender=" + executor + " Appeal Accepted");
                    Map<String, Object> data = SGCraftLinkCommon.createCommandsData(
                            "cmi jail " + username + " " + period + " r:Appeal Accepted",
                            "cmi clear " + username,
                            "xcatch clear " + username
                    );
                    plugin.getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, server, data, (status, content) -> {
                        if (status == 200) {
                            plugin.getLoggerProvider().info("Jailed and cleared " + username);
                        }
                    });
                    if (pm != null) {
                        pm.sendMessage("Your appeal has been accepted. You have been unbanned but your inventory has been cleared and you have been jailed for " + hours + " hours on " + server + " by " + executor).queue();
                    }
                }
                break;
            }
        }

        event.getMessage()
            .editMessage(event.getMessage().getContentRaw() + " Response: " + args[0] + " | Executor: " + executor)
            .setActionRows(event.getMessage().getActionRows().stream().map(ActionRow::asDisabled).collect(Collectors.toList()))
            .queue();

        event.reply("Response: " + args[0]).setEphemeral(true).queue();
    }
}
