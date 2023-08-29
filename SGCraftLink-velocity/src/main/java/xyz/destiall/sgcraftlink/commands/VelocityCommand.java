package xyz.destiall.sgcraftlink.commands;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.CommandProvider;
import xyz.destiall.sgcraftlink.providers.TabProvider;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityConsolePlayer;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityPlayer;

import java.util.Collections;
import java.util.List;

public class VelocityCommand implements SimpleCommand {
    private final CommandProvider cmd;
    private final SGCraftLinkVelocity plugin;
    private final CommandMeta meta;

    public VelocityCommand(SGCraftLinkVelocity plugin, String command, CommandProvider provider) {
        this.plugin = plugin;
        this.cmd = provider;

        meta = plugin.getProxy().getCommandManager().metaBuilder(command).plugin(plugin).build();
        plugin.getProxy().getCommandManager().register(meta, this);
    }

    public CommandMeta getMeta() {
        return meta;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.source() instanceof Player) {
            cmd.execute(new VelocityPlayer((Player) invocation.source()), invocation.arguments());
        } else {
            cmd.execute(new VelocityConsolePlayer(plugin.getProxy()), invocation.arguments());
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (cmd instanceof TabProvider) {
            if (invocation.source() instanceof Player) {
                return ((TabProvider) cmd).tab(new VelocityPlayer((Player) invocation.source()), invocation.arguments());
            }
            return ((TabProvider) cmd).tab(new VelocityConsolePlayer(plugin.getProxy()), invocation.arguments());
        }
        return Collections.emptyList();
    }
}
