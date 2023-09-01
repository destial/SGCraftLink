package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CommandLoggerListener {
    protected final PluginProvider plugin;

    public CommandLoggerListener(PluginProvider plugin) {
        this.plugin = plugin;
    }

    protected boolean parseCommand(PlayerProvider player, String message, boolean tab) {
        message = message.replaceFirst("/", "");
        List<String> command = new ArrayList<>(Arrays.asList(message.split(" ")));

        //Ethical reasons :)
        List<String> unloggedCommands = new ArrayList<>();
        unloggedCommands.add("authme");
        unloggedCommands.add("login");
        unloggedCommands.add("register");
        unloggedCommands.add("changepassword");
        unloggedCommands.add("changepass");
        for (String check : unloggedCommands) {
            if (command.get(0).toLowerCase().startsWith(check)) {
                return false;
            }
        }

        List<String> consoleOnlyCommands = plugin.getConfigProvider().getConfig().getStringList("console-only-commands");
        if (consoleOnlyCommands != null && !consoleOnlyCommands.isEmpty()) {
            for (String check : consoleOnlyCommands) {
                if (command.get(0).equalsIgnoreCase(check)) {
                    if (!tab) {
                        player.sendMessage(ColorProvider.of("&cYou do not have permission to use this command!"));
                    }
                    return true;
                }
            }
        }

        if (plugin.getDiscordProvider() == null)
            return false;

        TextChannel channel = plugin.getDiscordProvider().getJda().getTextChannelById(plugin.getConfigProvider().getConfig().getString("logged-commands-channel"));
        if (channel == null)
            return false;

        for (String check : plugin.getConfigProvider().getConfig().getStringList("logged-commands")) {
            if (command.get(0).toLowerCase().startsWith(check.toLowerCase())) {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setColor(Color.MAGENTA);
                builder.setAuthor(player.getName());
                builder.setDescription("/" + message);
                builder.setFooter("Server: " + player.getServer());
                channel.sendMessageEmbeds(builder.build()).queue();
                return false;
            }
        }

        return false;
    }
}
