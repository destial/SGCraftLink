package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.providers.DiscordProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

public final class ConsoleCommandListener extends ListenerAdapter {
    private final PluginProvider plugin;
    private final DiscordProvider discordProvider;

    public ConsoleCommandListener(PluginProvider plugin, DiscordProvider discordProvider) {
        this.plugin = plugin;
        this.discordProvider = discordProvider;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (discordProvider.getConsoleChannel() == null || !event.isFromGuild() || event.getAuthor().isBot())
            return;

        try {
            if (!event.getTextChannel().getId().equals(discordProvider.getConsoleChannel().getId()))
                return;

            try {
                discordProvider.getConsoleLogger().flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            String message = event.getMessage().getContentRaw();
            plugin.dispatchCommand(message);
        } catch (Exception ignored) {}
    }
}
