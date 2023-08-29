package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.LitebansDatabase;
import xyz.destiall.sgcraftlink.proxy.litebans.Punishment;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DiscordCommandListener extends ListenerAdapter {
    private final PluginProvider plugin;

    public DiscordCommandListener(PluginProvider plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.isFromGuild()) {
            String prefix = plugin.getConfigProvider().getString("command-prefix");

            if (!e.getMessage().getContentDisplay().startsWith(prefix)) return;
            if (e.getMember() == null || e.getAuthor().isBot() || !e.getMember().hasPermission(Permission.KICK_MEMBERS))
                return;

            String[] args = e.getMessage().getContentRaw().split(" ");
            String command = args[0].trim().toLowerCase().substring(prefix.length());
            if (command.startsWith("history") && args.length > 1) {
                String playerName = args[1].trim();
                if (playerName.isEmpty()) {
                    e.getChannel().sendMessageEmbeds(new EmbedBuilder().setTitle("Usage is: " + prefix + "history [playerName]").setColor(Color.RED).build()).queue();
                    return;
                }
                sendHistory(playerName, e.getChannel());
            } else if (command.startsWith("alts") && args.length > 1) {
                String playerName = args[1].trim();
                if (playerName.isEmpty()) {
                    e.getChannel().sendMessageEmbeds(new EmbedBuilder().setTitle("Usage is: " + prefix + "alts [playerName]").setColor(Color.RED).build()).queue();
                    return;
                }
                sendAlts(playerName, e.getChannel());
            } else if (command.startsWith("firstjoin") && args.length > 1) {
                String playername = args[1].trim();
                sendPlaytime(playername, e.getChannel());
            } else if (command.startsWith("nummessages") && args.length > 1) {
                List<User> mentioned = e.getMessage().getMentionedUsers();
                if (mentioned.isEmpty()) {
                    e.getChannel().sendMessageEmbeds(new EmbedBuilder().setTitle("Usage is: " + prefix + "nummessages @user").setColor(Color.RED).build()).queue();
                    return;
                }
                User user = mentioned.get(0);
                EmbedBuilder builder = new EmbedBuilder();
                builder.setDescription("Retrieving all messages... Please wait!");
                e.getChannel().sendMessageEmbeds(builder.build()).queue(m ->
                        plugin.getDiscordProvider().getMessagesSize(user, i -> {
                                    builder.setDescription(user.getAsTag() + " has " + format(i) + " messages.");
                                    m.editMessageEmbeds(builder.build()).queue();
                                }
                        ));
            } else if (command.startsWith("ipwhitelist") && args.length > 1) {
                String ip = args[1];
                String cmd = "bs whitelist ";
                if (ip.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
                    cmd += "add " + ip;
                } else {
                    cmd += "usernames add " + ip;
                }
                plugin.dispatchCommand(cmd);
                e.getChannel().sendMessage("Whitelisted " + ip).queue();
            } else if (command.startsWith("changepw") && args.length > 1) {
                String name = args[1];
                String cmd = "authme changepass " + name + " changepassword";
                Map<String, String> data = new HashMap<>();
                data.put("command", cmd);
                if (!plugin.getDiscordProvider().getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, "lobby", data, (res) -> {
                    if (res) {
                        e.getChannel().sendMessage("Changed password of " + name + " to ||changepassword||").queue();
                        return;
                    }
                    e.getChannel().sendMessage("Unable to change password of " + name).queue();
                })) {
                    e.getChannel().sendMessage("Changing of passwords is not available at this time!").queue();
                }
            }
        }
    }

    private String format(int number) {
        String raw = String.valueOf(number);
        int index = raw.length();
        int has3 = 0;
        for (int i = index; i != 0; i--) {
            if (has3++ == 3) {
                has3 = 0;
                String front = raw.substring(0, i);
                String back = raw.substring(i);
                raw = front + "," + back;
                i++;
            }
        }
        return raw;
    }

    private void sendPlaytime(String name, MessageChannel channel) {
        long firstJoin = LitebansDatabase.getFirstJoin(name);
        if (firstJoin != Long.MAX_VALUE) {
            long previous = plugin.getPlaytimeProvider().getFirstJoin(name);
            if (previous > firstJoin) {
                plugin.getPlaytimeProvider().setFirstJoin(name, firstJoin);
            } else {
                firstJoin = previous;
            }
        } else {
            firstJoin = plugin.getPlaytimeProvider().getFirstJoin(name);
        }

        if (firstJoin == Long.MAX_VALUE) {
            channel.sendMessage(name + " does not have a valid join date!").queue();
            return;
        }

        String d = String.valueOf(firstJoin);
        String substring = d.substring(0, d.length() - 3);
        String date = "<t:" + substring + ":f>";
        String relative = "<t:" + substring + ":R>";

        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription(name + " has played since " + date + "\n" + relative);
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    private void sendHistory(String playerName, MessageChannel channel) {
        UUID uuid = LitebansDatabase.getUUID(playerName);
        if (uuid == null) {
            channel.sendMessageEmbeds(new EmbedBuilder().setTitle("Invalid user!").setColor(Color.RED).build()).queue();
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
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    private void sendAlts(String playerName, MessageChannel channel) {
        UUID uuid = LitebansDatabase.getUUID(playerName);
        if (uuid == null) {
            channel.sendMessageEmbeds(new EmbedBuilder().setTitle("Invalid user!").setColor(Color.RED).build()).queue();
            return;
        }
        Set<String> alts = LitebansDatabase.getAlts(uuid.toString());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Alts");
        builder.setColor(Color.CYAN);
        builder.setDescription(String.join(", ", alts));
        channel.sendMessageEmbeds(builder.build()).queue();
    }
}
