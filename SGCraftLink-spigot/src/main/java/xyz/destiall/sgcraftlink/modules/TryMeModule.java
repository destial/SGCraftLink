package xyz.destiall.sgcraftlink.modules;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

public class TryMeModule extends Module implements CommandExecutor {
    public TryMeModule(SGCraftLinkSpigot plugin) {
        super(plugin, null);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return false;
    }
}
