package xyz.destiall.sgcraftlink.modules.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.awt.*;

public class DiscordSRVHook extends Module implements CommandExecutor {
    private final String announcementsId;

    public DiscordSRVHook(SGCraftLinkSpigot plugin) {
        super(plugin, "news.yml");
        announcementsId = config.getString("news-channel-id");
        DiscordSRV.api.subscribe(this);
        plugin.getCommand("sgcdiscord").setExecutor(this);
    }

    @Subscribe
    public void discordMessageReceived(DiscordGuildMessageReceivedEvent event) {
        if (announcementsId == null)
            return;

        if (!event.getMessage().getChannel().getId().equals(announcementsId))
            return;

        TextComponent message = format(event.getMessage(), config.getString("news-message", "&c&l[NEWS] &bThere is a new announcement on #{channel}!"));
        TextComponent hoverMessage = format(event.getMessage(), config.getString("news-message-hover"));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new TextComponent[] { hoverMessage })));
        if (config.getBoolean("news-message-hover-link")) {
            message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, event.getMessage().getJumpUrl()));
        }

        plugin.getServer().getOnlinePlayers().forEach(player -> {
            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.5f, 1);
            player.spigot().sendMessage(message);
        });
    }

    public TextChannel getTextChannel(String id) {
        return DiscordSRV.getPlugin().getJda().getTextChannelById(id);
    }

    private TextComponent format(Message message, String s) {
        return color(s.replace("{message}", message.getContentStripped())
                .replace("{channel}", message.getChannel().getName()));
    }

    private TextComponent color(String s) {
        return new TextComponent(SGCraftLinkSpigot.color( s));
    }

    @Override
    public void loadDefaultConfig() {
        config.set("news-channel-id", "709232194589163530");
        config.set("news-message", "&c&l[NEWS] &bThere is a new announcement on #{channel}!");
        config.set("news-message-hover", "&bClick to view it!");
        config.set("news-message-hover-link", true);
    }

    @Override
    public void disable() {
        DiscordSRV.api.unsubscribe(this);
        plugin.getCommand("sgcdiscord").setExecutor(null);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /sgcdiscord [rankup|rankdown] [player] [rank]");
            return false;
        }

        String playerName = args[1];

        if (args[0].equalsIgnoreCase("rankup") && args.length > 2) {
            EmbedBuilder embed = new EmbedBuilder();
            String rank = args[2];
            embed.setAuthor(playerName + " has ranked up to " + rank, null, DiscordSRV.getAvatarUrl(playerName, null));
            embed.setColor(Color.ORANGE);
            DiscordSRV.getPlugin().getMainTextChannel().sendMessageEmbeds(embed.build()).queue();
            return false;
        }

        if (args[0].equalsIgnoreCase("rankdown") && args.length > 2) {
            EmbedBuilder embed = new EmbedBuilder();
            String rank = args[2];
            embed.setAuthor(playerName + " has ranked down to " + rank, null, DiscordSRV.getAvatarUrl(playerName, null));
            embed.setColor(Color.ORANGE);
            DiscordSRV.getPlugin().getMainTextChannel().sendMessageEmbeds(embed.build()).queue();
            return false;
        }

        sender.sendMessage("Usage: /sgcdiscord [rankup|rankdown] [player] [rank]");
        return false;
    }
}
