package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.LitebansDatabase;
import xyz.destiall.sgcraftlink.proxy.litebans.Punishment;
import xyz.destiall.sgcraftlink.proxy.misc.Pair;

import java.awt.*;
import java.time.Duration;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
                sendPlaytime(plugin, playername, e.getChannel());
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

                String cmd = "authme password " + name + " changepassword";
                Map<String, String> data = new HashMap<>();
                data.put("command", cmd);
                if (!plugin.getDiscordProvider().getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, "lobby", data, (res) -> {
                    if (res.getKey() == 200) {
                        e.getChannel().sendMessage("Changed password of " + name + " to ||changepassword||").queue();
                        return;
                    }
                    e.getChannel().sendMessage("Unable to change password of " + name).queue();
                })) {
                    e.getChannel().sendMessage("Changing of passwords is not available at this time!").queue();
                }
            } else if (command.startsWith("chathistory") && args.length > 1) {
                String name = args[1];
                int limit = args.length > 2 ? Integer.parseInt(args[2]) : 50;
                sendChatHistory(plugin, e.getMember(), name, e.getChannel(), limit);
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

    public static void sendPlaytime(PluginProvider plugin, String name, MessageChannel channel) {
        UUID uuid = checkUserExists(name, channel);
        if (uuid == null)
            return;

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

    public static void sendHistory(String playerName, MessageChannel channel) {
        UUID uuid = checkUserExists(playerName, channel);
        if (uuid == null)
            return;

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

    public static UUID checkUserExists(String playerName, MessageChannel channel) {
        UUID uuid = LitebansDatabase.getUUID(playerName);
        if (uuid == null) {
            channel.sendMessageEmbeds(new EmbedBuilder().setTitle("Invalid user!").setColor(Color.RED).build()).queue();
            return null;
        }
        return uuid;
    }

    public static void sendAlts(String playerName, MessageChannel channel) {
        UUID uuid = checkUserExists(playerName, channel);
        if (uuid == null)
            return;

        Set<String> alts = LitebansDatabase.getAlts(uuid.toString());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Alts");
        builder.setColor(Color.CYAN);
        builder.setDescription(String.join(", ", alts));
        channel.sendMessageEmbeds(builder.build()).queue();
    }

    public static void sendChatHistory(PluginProvider plugin, Member requester, String playerName, MessageChannel channel, int limit) {
        UUID uuid = checkUserExists(playerName, channel);
        if (uuid == null)
            return;

        List<Pair<Long, String>> chatMessages = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("limit", limit);
        AtomicInteger test = new AtomicInteger(0);
        Collection<String> servers = plugin.getDiscordProvider().getDataManager().getServers();
        for (String server : servers) {
            plugin.getDiscordProvider().getDataManager().sendData(SGCraftLinkCommon.DATA_CHATS, server, data, (result) -> {
                if (result.getKey() == 200) {
                    JSONObject obj = new JSONObject(result.getValue());
                    JSONArray array = obj.getJSONArray("messages");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject msg = array.getJSONObject(i);
                        long time = msg.getLong("time");
                        chatMessages.add(new Pair<>(time, "<t:" + time + ":R> [" + server + "] " + playerName + ": " + msg.getString("msg")));
                    }

                    chatMessages.sort(Comparator.comparingLong(Pair<Long,String>::getKey).reversed());
                    if (test.incrementAndGet() >= servers.size()) {
                        EmbedBuilder embed = new EmbedBuilder();
                        embed.setColor(Color.RED);
                        embed.setTitle("Chat History of " + playerName);
                        embed.setFooter("Requested by " + requester.getEffectiveName());
                        StringBuilder text = new StringBuilder();
                        for (Pair<Long, String> messages : chatMessages) {
                            if (text.length() + messages.getValue().length() > 2048) {
                                embed.setDescription(text);
                                channel.sendMessageEmbeds(embed.build()).queue();
                                embed.setDescription("");
                                text = new StringBuilder();
                            }
                            text.append(messages.getValue()).append("\n");
                        }

                        if (text.length() > 2048) {
                            text.delete(2044, text.length() - 1);
                            text.append("...");
                        }
                        embed.setDescription(text);
                        channel.sendMessageEmbeds(embed.build()).queue();

                    }
                }
            });
        }
    }
}
