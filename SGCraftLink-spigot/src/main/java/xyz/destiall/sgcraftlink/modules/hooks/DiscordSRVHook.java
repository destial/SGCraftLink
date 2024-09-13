package xyz.destiall.sgcraftlink.modules.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import jdk.internal.joptsimple.internal.Strings;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.events.DataReceiveEvent;
import xyz.destiall.sgcraftlink.modules.Module;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class DiscordSRVHook extends Module implements Listener, CommandExecutor {
    public DiscordSRVHook(SGCraftLinkSpigot plugin) {
        super(plugin, "news.yml");
        setCommandExecutor("sgcdiscord");
    }

    @EventHandler
    public void onDataReceive(DataReceiveEvent e) {
        JSONObject json = e.getJson();
        switch (e.getDataType()) {
            case SGCraftLinkCommon.DATA_LINK_DISCORDSRV: {
                JSONArray linked = json.getJSONArray("linked");

                for (int i = 0; i < linked.length(); i++) {
                    JSONObject link = linked.getJSONObject(i);
                    String username = link.getString("username");
                    String userId = link.getString("id");
                    OfflinePlayer player = plugin.getServer().getOfflinePlayer(username);
                    DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().put(userId, player.getUniqueId());
                }

                try {
                    DiscordSRV.getPlugin().getAccountLinkManager().save();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                break;
            }
            case SGCraftLinkCommon.DATA_UNLINK_DISCORDSRV: {
                JSONArray linked = json.getJSONArray("unlinked");

                for (int i = 0; i < linked.length(); i++) {
                    JSONObject link = linked.getJSONObject(i);
                    String userId = link.getString("id");
                    DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().remove(userId);
                }

                try {
                    DiscordSRV.getPlugin().getAccountLinkManager().save();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                break;
            }
            default: break;
        }
    }

    @Subscribe
    public void discordMessageReceived(DiscordGuildMessageReceivedEvent event) {
        if (!config.contains("news-channel-id"))
            return;

        if (config.isString("news-channel-id")) {
            if (!event.getMessage().getChannel().getId().equals(config.getString("news-channel-id")))
                return;
        } else if (config.isList("news-channel-id")) {
            if (!config.getStringList("news-channel-id").contains(event.getMessage().getChannel().getId()))
                return;
        }

        TextComponent message = format(event.getMessage(), config.getString("news-message", "&c&l[NEWS] &bThere is a new announcement on #{channel}!"));
        TextComponent hoverMessage = format(event.getMessage(), config.getString("news-message-hover"));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new TextComponent[] { hoverMessage })));
        if (config.getBoolean("news-message-hover-link")) {
            message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, event.getMessage().getJumpUrl()));
        }

        plugin.getServer().getOnlinePlayers().forEach(player -> {
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.5f, 1);
            } catch (Exception ignored) {}
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
        return new TextComponent(SGCraftLinkSpigot.color(s));
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("news-channel-id", "709232194589163530");
        config.set("news-message", "&c&l[NEWS] &bThere is a new announcement on #{channel}!");
        config.set("news-message-hover", "&bClick to view it!");
        config.set("news-message-hover-link", true);
    }

    @Override
    public void disable() {
        DiscordSRV.api.unsubscribe(this);
        unsetCommandExecutor("sgcdiscord");
    }

    @Override
    public void enable() {
        DiscordSRV.api.subscribe(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /sgcdiscord [rankup|rankdown|saveaccounts|embed|reload] [player|color] [rank|msg]");
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

        if (args[0].equalsIgnoreCase("embed")) {
            EmbedBuilder embed = new EmbedBuilder();
            int start = 2;
            try {
                Color color = Color.decode(args[1]);
                embed.setColor(color);
            } catch (Exception e) {
                start = 1;
            }
            String msg = Strings.join(Arrays.copyOfRange(args, start, args.length), " ");
            embed.setAuthor(msg);
            DiscordSRV.getPlugin().getMainTextChannel().sendMessageEmbeds(embed.build()).queue();
            return false;
        }

        if (args[0].equalsIgnoreCase("saveaccounts")) {
            try {
                DiscordSRV.getPlugin().getAccountLinkManager().save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            sender.sendMessage(color("&bSaved linked accounts!"));
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            loadConfig();
            return false;
        }

        sender.sendMessage("Usage: /sgcdiscord [rankup|rankdown|saveaccounts|reload] [player] [rank]");
        return false;
    }
}
