package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.LitebansDatabase;
import xyz.destiall.sgcraftlink.proxy.litebans.Punishment;

import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.proxy.listeners.DiscordCommandListener.sendChatHistory;

public class ReportButtonListener extends ListenerAdapter {
    private final PluginProvider plugin;

    public ReportButtonListener(PluginProvider plugin) {
        this.plugin = plugin;
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

        if (!event.getChannel().getId().equals("1097754814223487037")) {
            super.onButtonInteraction(event);
            return;
        }

        MessageEmbed embed = event.getMessage().getEmbeds().get(0);
        MessageEmbed.Footer footer = embed.getFooter();
        String reporter = "none";
        if (footer != null && footer.getText() != null) {
            reporter = footer.getText();
            reporter = reporter.substring(reporter.lastIndexOf(" ")).trim();
        }

        User user = event.getUser();
        String staff = plugin.getLinkProvider().getUsername(user);
        if (staff == null) {
            Member member = event.getMember();
            if (member != null) {
                staff = member.getEffectiveName();
            } else {
                staff = user.getName();
            }
        }

        PlayerProvider player = plugin.getPlayer(reporter);
        switch (id) {
            case "sgc-report-accepted": {
                event.reply("Report acknowledged!").setEphemeral(true).queue();
                event.getMessage()
                        .editMessage(user.getAsMention() + " will look into this")
                        .setActionRows(event.getMessage().getActionRows().stream().map(ActionRow::asDisabled).collect(Collectors.toList()))
                        .queue();
                if (player != null) {
                    player.sendMessage(ColorProvider.of("&6&l[REPORT]&e " + staff + " has &aacknowledged&e your report!"));
                }
                User reporterUser = plugin.getLinkProvider().getUser(reporter);
                if (reporterUser != null) {
                    String finalStaff = staff;
                    reporterUser.openPrivateChannel().queue(pm -> {
                        pm.sendMessage(finalStaff + " has acknowledged your report!").queue();
                    });
                }
                break;
            }
            case "sgc-report-denied": {
                event.reply("Report denied!").setEphemeral(true).queue();
                event.getMessage()
                        .editMessage(user.getAsMention() + " denied this")
                        .setActionRows(event.getMessage().getActionRows().stream().map(ActionRow::asDisabled).collect(Collectors.toList()))
                        .queue();
                if (player != null) {
                    player.sendMessage(ColorProvider.of("&6&l[REPORT]&e " + staff + " has &cdenied&e your report!"));
                }
                User reporterUser = plugin.getLinkProvider().getUser(reporter);
                if (reporterUser != null) {
                    String finalStaff = staff;
                    reporterUser.openPrivateChannel().queue(pm -> {
                        pm.sendMessage(finalStaff + " has denied your report!").queue();
                    });
                }
                break;
            }
            case "sgc-report-history":
                UUID uuid = LitebansDatabase.getUUID(embed.getTitle());
                if (uuid == null) {
                    event.reply("User not found!").setEphemeral(true).queue();
                    return;
                }

                List<Punishment> history = LitebansDatabase.getHistory(uuid.toString());
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle("History of " + LitebansDatabase.getPlayerName(uuid.toString()));
                builder.setFooter("Will only show latest 24 history");
                builder.setColor(Color.CYAN);
                int i = 0;
                for (Punishment p : history) {
                    ++i;
                    if (i >= 24) break;
                    builder.addField(p.getType().name() + " (" + p.getDuration() + ") " + (p.isActive() ? "Active" : "Inactive"), p.getReason() + " *(" + p.getExecutor() + ")*", false);
                }

                event.replyEmbeds(builder.build()).queue();
                break;
            case "sgc-report-chathistory": {
                int limit = 50;
                event.deferReply().queue(r -> {
                    sendChatHistory(plugin, event.getMember(), embed.getTitle(), limit, (consumer) ->
                        r.editOriginalEmbeds(consumer).queue()
                    );
                });
                break;
            }
            default: {
                super.onButtonInteraction(event);
                break;
            }
        }
    }
}
