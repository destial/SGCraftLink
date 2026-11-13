package xyz.destiall.sgcraftlink.proxy.botsentry;

import com.lahuca.botsentry.api.BotSentryAPI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

public abstract class BotSentryManager {
    private final PluginProvider plugin;
    private final BotSentryAPI api;

    public BotSentryManager(PluginProvider plugin) {
        this.plugin = plugin;
        api = BotSentryAPI.getAPI();
    }

    public void onIpBlacklisted(final PlayerProvider player) {
        plugin.getDiscordProvider().getAdminChannel().sendResponseNotification(
                player.getName() + " tried to join but their IP is blacklisted! (" + player.getIP() + ")\n" +
                        "Do you want to whitelist their username?"
                , (pair) -> {
            if (!pair.getKey())
                return;

            MessageEmbed embed = pair.getValue().getEmbeds().get(0);
            EmbedBuilder builder = new EmbedBuilder(embed);
            builder.setDescription("Added to the IP whitelist of user " + player.getName());
            pair.getValue().editMessageEmbeds(builder.build()).queue();

            plugin.dispatchCommand("bs whitelist usernames add " + player.getName());
        });
    }
}
