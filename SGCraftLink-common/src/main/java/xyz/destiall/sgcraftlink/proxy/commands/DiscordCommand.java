package xyz.destiall.sgcraftlink.proxy.commands;

import net.dv8tion.jda.api.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.providers.ColorProvider;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.PlayerProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentClickProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentHoverProvider;
import xyz.destiall.sgcraftlink.providers.component.ComponentProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DiscordCommand implements CommandProvider, TabProvider {
    private final PluginProvider plugin;

    public DiscordCommand(PluginProvider plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(PlayerProvider player, String[] args) {
        if (args == null || args.length == 0) {
            String link = plugin.getConfigProvider().getString("invite-url");
            if (link == null) {
                link = "https://discord.gg/DpGKMW7qbG";
            }
            ComponentProvider<?> message = ComponentProvider.of("Join our discord! " + link);
            message.setColor(ColorProvider.ChatColor.AQUA);
            message.setClick(ComponentClickProvider.of(ComponentClickProvider.Action.OPEN_URL, link));
            message.setHover(ComponentHoverProvider.of(ComponentHoverProvider.Action.SHOW_TEXT, ColorProvider.of("&bClick to open the link")));
            player.sendMessageComponent(message);

            if (!plugin.getLinkProvider().isLinked(player.getName())) {
                ComponentProvider<?> message2 = ComponentProvider.of("/discord link (Link your Discord account to your Minecraft account)");
                message2.setColor(ColorProvider.ChatColor.YELLOW);
                message2.setClick(ComponentClickProvider.of(ComponentClickProvider.Action.RUN_COMMAND, "/discord link"));
                message2.setHover(ComponentHoverProvider.of(ComponentHoverProvider.Action.SHOW_TEXT, ColorProvider.of("&bClick to link your account")));
                player.sendMessageComponent(message2);
            }
            return;
        }

        if (args[0].equalsIgnoreCase("link")) {
            if (plugin.getLinkProvider().isLinked(player.getName())) {
                User user = plugin.getLinkProvider().getUser(player.getName());
                ComponentProvider<?> message = ComponentProvider.of("You are already linked to " + user.getAsTag());
                message.setColor(ColorProvider.ChatColor.RED);
                player.sendMessageComponent(message);
                return;
            }

            final int code = plugin.getLinkProvider().generateCode(player.getName());

            ComponentProvider<?> message = ComponentProvider.of("This is your code: " + code + "\n");
            message.setColor(ColorProvider.ChatColor.AQUA);
            message.setHover(ComponentHoverProvider.of(ComponentHoverProvider.Action.SHOW_TEXT, "Click to copy code"));
            message.setClick(ComponentClickProvider.of(ComponentClickProvider.Action.COPY_TO_CLIPBOARD, "" + code));
            ComponentProvider<?> message2 = ComponentProvider.of("Privately message " + plugin.getDiscordProvider().getJda().getSelfUser().getAsTag() + " the code to link your account.");
            message2.setColor(ColorProvider.ChatColor.AQUA);
            ComponentProvider<?> message3 = ComponentProvider.of("You have 5 minutes to respond, or this code will be expired!");
            message3.setColor(ColorProvider.ChatColor.GOLD);
            player.sendMessageComponent(message, message2, message3);

            plugin.getScheduler().schedule(() -> {
                if (plugin.getLinkProvider().invalidate(code)) {
                    ComponentProvider<?> invalidated = ComponentProvider.of("Your code has expired!");
                    invalidated.setColor(ColorProvider.ChatColor.RED);
                    player.sendMessageComponent(invalidated);
                }
            }, 5, TimeUnit.MINUTES);

        } else if (args[0].equalsIgnoreCase("unlink")) {
            if (!plugin.getLinkProvider().isLinked(player.getName())) {
                ComponentProvider<?> message = ComponentProvider.of("You are not linked!");
                message.setColor(ColorProvider.ChatColor.RED);
                player.sendMessageComponent(message);
                return;
            }

            plugin.getLinkProvider().unlink(player);
            ComponentProvider<?> message = ComponentProvider.of("You have unlinked your account!");
            message.setColor(ColorProvider.ChatColor.RED);
            player.sendMessageComponent(message);
        } else if (args[0].equalsIgnoreCase("adminupdate") && player.hasPermission("sgcdiscord.admin")) {
            JSONObject object = new JSONObject();
            JSONArray linked = new JSONArray();
            for (String username : plugin.getLinkProvider().getDatabase()) {
                String id = plugin.getLinkProvider().getId(username);
                linked.put(new JSONObject().put("username", username).put("id", id));
            }
            object.put("linked", linked);
            plugin.getDataManager().sendDataToAll(null, SGCraftLinkCommon.DATA_LINK_DISCORDSRV, object, (result) -> {});
            ComponentProvider<?> message = ComponentProvider.of("Updated individual servers' DiscordSRV linked accounts!");
            message.setColor(ColorProvider.ChatColor.RED);
            player.sendMessageComponent(message);
        } else {
            ComponentProvider<?> message = ComponentProvider.of("Usages:\n /discord link (Link your Discord account to your Minecraft account)\n /discord unlink (Unlink your Discord account from your Minecraft account)");
            message.setColor(ColorProvider.ChatColor.RED);
            player.sendMessageComponent(message);
        }
    }

    @Override
    public List<String> tab(PlayerProvider sender, String[] args) {
        List<String> usage = Arrays.asList("link", "unlink");
        if (args == null || args.length == 0) {
            return usage;
        }
        if (args.length > 1) return Collections.emptyList();
        return usage.stream().filter(a -> a.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
    }
}
