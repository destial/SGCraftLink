package xyz.destiall.sgcraftlink.proxy.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.litebans.LitebansDatabase;
import xyz.destiall.sgcraftlink.proxy.litebans.Punishment;
import xyz.destiall.sgcraftlink.proxy.misc.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DiscordCommandListener extends ListenerAdapter {
    private JDA jda;
    private final PluginProvider plugin;
    private final List<Command> slashCommands;

    public DiscordCommandListener(PluginProvider plugin) {
        this.plugin = plugin;
        slashCommands = new ArrayList<>();
    }

    public void registerCommands(JDA jda) {
        this.jda = jda;
        SlashCommandData chatHistory = Commands.slash("chathistory", "Get the chat history of a player!")
                .addOption(OptionType.STRING, "player", "The player to check", true)
                .addOption(OptionType.NUMBER, "limit", "The amount of chat messages", false);

        SlashCommandData history = Commands.slash("history", "Get the punishment history of a player")
                .addOption(OptionType.STRING, "player", "The player to check", true);

        SlashCommandData alts = Commands.slash("alts", "Get the alts of a player")
                .addOption(OptionType.STRING, "player", "The player to check", true);

        SlashCommandData firstjoin = Commands.slash("firstjoin", "The time when the player first joined")
                .addOption(OptionType.STRING, "player", "The player to check", true);

        SlashCommandData ipwhitelist = Commands.slash("ipwhitelist", "The IP or username to whitelist")
                .addOption(OptionType.STRING, "playerorip", "The player or IP to add", true);

        SlashCommandData changepw = Commands.slash("changepw", "Change the password of a player")
                .addOption(OptionType.STRING, "player", "The player to check", true)
                .addOption(OptionType.STRING, "password", "The new password", false);

        jda.updateCommands().addCommands(chatHistory, history, alts, firstjoin, ipwhitelist, changepw).queue(slashCommands::addAll);

        plugin.getScheduler().schedule(() -> {
            plugin.getDiscordProvider().getJda().retrieveCommandById(slashCommands.get(0).getId()).queue(command -> {},
                    error -> registerCommands(jda));
        }, 3, 3, TimeUnit.HOURS);
    }

    public void unregisterCommands() {
        for (Command command : slashCommands) {
            jda.deleteCommandById(command.getId()).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent e) {
        if (!e.isFromGuild()) {
            super.onSlashCommandInteraction(e);
            return;
        }

        Command command = slashCommands.stream().filter(c -> c.getId().equalsIgnoreCase(e.getCommandId())).findFirst().orElse(null);
        if (command == null) {
            super.onSlashCommandInteraction(e);
            return;
        }

        if (e.getMember() == null || !e.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            e.reply("You do not have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        switch (command.getName()) {
            case "history": {
                String playerName = e.getOption("player", OptionMapping::getAsString);
                e.replyEmbeds(sendHistory(playerName).build()).queue();
                break;
            }
            case "alts": {
                String playerName = e.getOption("player", OptionMapping::getAsString);
                e.replyEmbeds(sendAlts(playerName).build()).queue();
                break;
            }
            case "firstjoin": {
                String playerName = e.getOption("player", OptionMapping::getAsString);
                e.replyEmbeds(sendPlaytime(plugin, playerName).build()).queue();
                break;
            }
            case "ipwhitelist": {
                String nameOrIp = e.getOption("playerorip", OptionMapping::getAsString);
                e.replyEmbeds(sendIPWhitelist(plugin, nameOrIp).build()).queue();
                break;
            }
            case "changepw": {
                String playerName = e.getOption("player", OptionMapping::getAsString);
                String password = e.getOption("password", (map) -> map == null ? "changepassword" : map.getAsString());
                e.deferReply(true).queue(r -> {
                    sendChangePW(plugin, playerName, password, (consumer) ->
                        r.editOriginalEmbeds(consumer.build()).queue()
                    );
                });
                break;
            }
            case "chathistory": {
                String name = e.getOption("player", OptionMapping::getAsString);
                Integer limit = e.getOption("limit", (fallback) -> 50);
                e.deferReply().queue(r -> {
                    sendChatHistory(plugin, e.getMember(), name, limit == null ? 50 : limit, (consumer) ->
                        r.editOriginalEmbeds(consumer).queue()
                    );
                });
                break;
            }
            default: {
                super.onSlashCommandInteraction(e);
                break;
            }
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!e.isFromGuild()) {
            return;
        }

        String prefix = plugin.getConfigProvider().getString("command-prefix");

        if (!e.getMessage().getContentDisplay().startsWith(prefix) ||
                e.getMember() == null || e.getAuthor().isBot() ||
                !e.getMember().hasPermission(Permission.KICK_MEMBERS))
            return;

        String[] args = e.getMessage().getContentRaw().split(" ");
        String command = args[0].trim().toLowerCase().substring(prefix.length());
        if (command.startsWith("history") && args.length > 1) {
            String playerName = args[1].trim();
            if (playerName.isEmpty()) {
                e.getMessage().replyEmbeds(new EmbedBuilder().setTitle("Usage is: " + prefix + "history [playerName]").setColor(Color.RED).build()).queue();
                return;
            }
            e.getMessage().replyEmbeds(sendHistory(playerName).build()).queue();
        } else if (command.startsWith("alts") && args.length > 1) {
            String playerName = args[1].trim();
            if (playerName.isEmpty()) {
                e.getMessage().replyEmbeds(new EmbedBuilder().setTitle("Usage is: " + prefix + "alts [playerName]").setColor(Color.RED).build()).queue();
                return;
            }
            e.getMessage().replyEmbeds(sendAlts(playerName).build()).queue();
        } else if (command.startsWith("firstjoin") && args.length > 1) {
            String playername = args[1].trim();
            e.getMessage().replyEmbeds(sendPlaytime(plugin, playername).build()).queue();
        } else if (command.startsWith("ipwhitelist") && args.length > 1) {
            String ip = args[1].trim();
            e.getMessage().replyEmbeds(sendIPWhitelist(plugin, ip).build()).queue();
        } else if (command.startsWith("changepw") && args.length > 1) {
            String name = args[1];
            String pw = "changepassword";
            if (args.length > 2) {
                pw = args[2];
            }

            sendChangePW(plugin, name, pw, (consumer) ->
                e.getMessage().replyEmbeds(consumer.build()).queue()
            );
        } else if (command.startsWith("chathistory") && args.length > 1) {
            String name = args[1];
            int limit = args.length > 2 ? Integer.parseInt(args[2]) : 50;
            sendChatHistory(plugin, e.getMember(), name, limit, (consumer) ->
                e.getMessage().replyEmbeds(consumer).queue()
            );
        }
    }

    public static void sendChangePW(PluginProvider plugin, String playerName, String pw, Consumer<EmbedBuilder> consumer) {
        if (checkUserExists(playerName) == null) {
            consumer.accept(new EmbedBuilder().setColor(Color.RED).setTitle("Invalid user!"));
            return;
        }
        Map<String, Object> data = SGCraftLinkCommon.createCommandsData("authme password " + playerName + " " + pw);
        if (!plugin.getDiscordProvider().getDataManager().sendData(SGCraftLinkCommon.DATA_EXECUTE_COMMAND, "lobby", data, (res) -> {
            if (res.getKey() == 200) {
                consumer.accept(new EmbedBuilder().setTitle("Changed password of " + playerName + " to ||" + pw + "||").setColor(Color.GREEN));
                return;
            }
            consumer.accept(new EmbedBuilder().setTitle("Unable to change password of " + playerName).setColor(Color.RED));
        })) {
            consumer.accept(new EmbedBuilder().setTitle("Changing of passwords is not available at this time!").setColor(Color.RED));
        }
    }

    public static EmbedBuilder sendIPWhitelist(PluginProvider plugin, String nameOrIp) {
        if (nameOrIp == null) {
            return new EmbedBuilder().setColor(Color.RED).setTitle("Invalid name or IP!");
        }
        String cmd = "bs whitelist ";
        if (nameOrIp.matches("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")) {
            cmd += "add " + nameOrIp;
        } else {
            cmd += "usernames add " + nameOrIp;
        }
        plugin.dispatchCommand(cmd);
        return new EmbedBuilder().setColor(Color.GREEN).setTitle("Whitelisted " + nameOrIp);
    }

    public static EmbedBuilder sendPlaytime(PluginProvider plugin, String name) {
        UUID uuid = checkUserExists(name);
        if (uuid == null)
            return new EmbedBuilder().setTitle("Invalid user!").setColor(Color.RED);;

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
            return new EmbedBuilder().setTitle(name + " does not have a valid join date!").setColor(Color.RED);
        }

        String d = String.valueOf(firstJoin);
        String substring = d.substring(0, d.length() - 3);
        String date = "<t:" + substring + ":f>";
        String relative = "<t:" + substring + ":R>";

        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription(name + " has played since " + date + "\n" + relative);
        return builder;
    }

    public static EmbedBuilder sendHistory(String playerName) {
        UUID uuid = checkUserExists(playerName);
        if (uuid == null)
            return new EmbedBuilder().setTitle("Invalid user!").setColor(Color.RED);

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
        return builder;
    }

    public static UUID checkUserExists(String playerName) {
        return playerName == null ? null : LitebansDatabase.getUUID(playerName);
    }

    public static EmbedBuilder sendAlts(String playerName) {
        UUID uuid = checkUserExists(playerName);
        if (uuid == null) {
            return new EmbedBuilder().setTitle("Invalid user!").setColor(Color.RED);
        }

        Set<String> alts = LitebansDatabase.getAlts(uuid.toString());
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Alts");
        builder.setColor(Color.CYAN);
        builder.setDescription(String.join(", ", alts));
        return builder;
    }

    public static void sendChatHistory(PluginProvider plugin, Member requester, String playerName, int limit, Consumer<List<MessageEmbed>> consumer) {
        UUID uuid = checkUserExists(playerName);
        if (uuid == null) {
            consumer.accept(Arrays.asList(new EmbedBuilder().setTitle("Invalid user!").setColor(Color.RED).build()));
            return;
        }

        List<Pair<Long, String>> chatMessages = new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        data.put("player", playerName);
        data.put("limit", limit);
        AtomicInteger test = new AtomicInteger(0);
        Collection<String> servers = plugin.getDiscordProvider().getDataManager().getServers();
        List<MessageEmbed> embeds = new ArrayList<>();
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
                                embeds.add(embed.build());
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
                        embeds.add(embed.build());

                        consumer.accept(embeds);
                    }
                }
            });
        }
    }
}
