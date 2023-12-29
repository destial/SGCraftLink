package xyz.destiall.sgcraftlink.proxy.litebans;

import litebans.api.Entry;
import litebans.api.Events;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.TimeFormat;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.awt.*;

public class PunishmentListener extends Events.Listener {
    private final PluginProvider plugin;

    private TextChannel channel;
    public PunishmentListener(PluginProvider plugin) {
        this.plugin = plugin;

        Events.get().register(this);
    }

    public void close() {
        Events.get().unregister(this);
    }

    public TextChannel getChannel() {
        if (channel == null) {
            channel = plugin.getDiscordProvider().getJda().getTextChannelById("1190320716080480276");
        }
        return channel;
    }

    @Override
    public void entryAdded(Entry entry) {
        PunishmentType type;
        switch (entry.getType()) {
            case "ban":
                type = entry.isIpban() ? PunishmentType.IPBAN : PunishmentType.BAN;
                break;
            case "mute":
                type = PunishmentType.MUTE;
                break;
            case "warn":
                type = PunishmentType.WARN;
                break;
            case "kick":
                type = PunishmentType.KICK;
                break;
            default: return;
        }
        String username = LitebansDatabase.getPlayerName(entry.getUuid());
        if (username == null)
            return;

        Punishment punishment = new Punishment(type, username, entry.getExecutorName(), entry.getDuration(), entry.getDateEnd(), entry.getReason(), entry.isActive(), entry.getServerOrigin());
        broadcastToDiscord(punishment, false);
    }

    @Override
    public void entryRemoved(Entry entry) {
        PunishmentType type;
        switch (entry.getType()) {
            case "ban":
                type = entry.isIpban() ? PunishmentType.IPBAN : PunishmentType.BAN;
                break;
            case "mute":
                type = PunishmentType.MUTE;
                break;
            case "warn":
                type = PunishmentType.WARN;
                break;
            default: return;
        }
        String username = LitebansDatabase.getPlayerName(entry.getUuid());
        if (username == null)
            return;

        Punishment punishment = new Punishment(type, username, entry.getExecutorName(), entry.getDuration(), entry.getDateEnd(), entry.getRemovalReason() != null ? entry.getRemovalReason() : entry.getReason(), entry.isActive(), entry.getServerOrigin());
        broadcastToDiscord(punishment, true);
    }

    public void broadcastToDiscord(Punishment punishment, boolean removed) {
        TextChannel channel = getChannel();
        if (channel == null)
            return;

        String title;
        if (removed) {
            title = punishment.getType().name() + " Removed";
        } else {
            title = punishment.getType().name() + " (" + punishment.getDuration() + ")";
        }
        String author = punishment.getVictim();
        String field = "Reason: " + MarkdownUtil.monospace(punishment.getReason()) +
                "\n" +
                "Punished by: " + MarkdownUtil.monospace(punishment.getExecutor()) +
                "\n" +
                "Server: " + MarkdownUtil.monospace(punishment.getServer()) +
                "\n" +
                "Time: " + TimeFormat.DATE_TIME_LONG.format(punishment.getMilliseconds()) + " (" + TimeFormat.RELATIVE.format(punishment.getMilliseconds()) + ")";
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.YELLOW);
        embed.setAuthor(author, null, "https://cravatar.eu/helmavatar/{username}/16.png".replace("{username}", author));
        embed.setTitle(title);
        embed.setDescription(field);

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
