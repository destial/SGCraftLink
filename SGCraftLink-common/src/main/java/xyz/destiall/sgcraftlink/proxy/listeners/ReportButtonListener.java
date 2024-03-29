package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.LitebansDatabase;

import java.util.List;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.proxy.listeners.DiscordCommandListener.sendChatHistory;
import static xyz.destiall.sgcraftlink.proxy.listeners.DiscordCommandListener.sendHistory;

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

        if (!event.getChannel().getId().equals(plugin.getConfigProvider().getConfig().getString("report-channel", "1210647775847649360"))) {
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

        String reported = embed.getTitle();
        String reason = embed.getDescription();

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
                    reporterUser.openPrivateChannel().queue(pm ->
                        pm.sendMessage(finalStaff + " has acknowledged your report!\n" + MarkdownUtil.monospace(reported + ": " + reason)).queue()
                    , err -> {});
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
                    reporterUser.openPrivateChannel().queue(pm ->
                        pm.sendMessage(finalStaff + " has denied your report!\n" + MarkdownUtil.monospace(reported + ": " + reason)).queue()
                    , err -> {});
                }
                break;
            }
            case "sgc-report-history":
                event.deferReply().queue(r -> {
                    List<EmbedBuilder> embeds = sendHistory(LitebansDatabase.getPlayerName(embed.getTitle()));
                    r.editOriginalEmbeds(embeds.stream().map(EmbedBuilder::build).collect(Collectors.toList())).queue();
                });
                break;
            case "sgc-report-chathistory": {
                event.deferReply().queue(r ->
                    sendChatHistory(plugin, event.getMember(), embed.getTitle(), 50, event.getMessage().getTimeCreated().toInstant().toEpochMilli(), (consumer) ->
                        r.editOriginalEmbeds(consumer).queue()
                    )
                );
                break;
            }
            default: {
                super.onButtonInteraction(event);
                break;
            }
        }
    }
}
