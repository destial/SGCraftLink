package xyz.destiall.sgcraftlink.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeeConsolePlayer;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeePlayer;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;

import java.util.Collections;

public class BungeeCommand extends Command implements TabExecutor {
    private final SGCraftLinkWaterfall plugin;
    private final CommandProvider cmd;
    public BungeeCommand(SGCraftLinkWaterfall plugin, String command, CommandProvider provider) {
        super(command);
        this.plugin = plugin;
        this.cmd = provider;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            cmd.execute(new BungeePlayer(player), args);
        } else {
            cmd.execute(new BungeeConsolePlayer(plugin.getProxy()), args);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (cmd instanceof TabProvider) {
            if (sender instanceof ProxiedPlayer) {
                return ((TabProvider) cmd).tab(new BungeePlayer((ProxiedPlayer) sender), args);
            }
            return ((TabProvider) cmd).tab(new BungeeConsolePlayer(plugin.getProxy()), args);
        }
        return Collections.emptyList();
    }
}
