package xyz.destiall.sgcraftlink.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Sound;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class DiscordSRVHook extends Module {
    private final String announcementsId;

    public DiscordSRVHook(SGCraftLinkSpigot plugin) {
        super(plugin, "news.yml");
        announcementsId = config.getString("news-channel-id");

        if (announcementsId == null)
            return;

        DiscordSRV.api.subscribe(this);
    }

    @Subscribe
    public void discordMessageReceived(DiscordGuildMessageReceivedEvent event) {
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

    private TextComponent format(Message message, String s) {
        if (announcementsId == null)
            return color(s);

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
    }
}
