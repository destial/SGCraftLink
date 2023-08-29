package xyz.destiall.sgcraftlink.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeePlayer;
import xyz.destiall.sgcraftlink.proxy.listeners.CommandLoggerListener;

public class BungeeCommandListener extends CommandLoggerListener implements Listener {
    public BungeeCommandListener(SGCraftLinkWaterfall plugin) {
        super(plugin);
    }

    @EventHandler
    public void onChatEvent(ChatEvent e) {
        if (!e.isCommand() || !(e.getSender() instanceof ProxiedPlayer))
            return;

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        if (parseCommand(new BungeePlayer(player), e.getMessage(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTabEvent(TabCompleteEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer))
            return;

        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        BungeePlayer bungeePlayer = new BungeePlayer(player);
        e.getSuggestions().removeIf(s -> parseCommand(bungeePlayer, s, true));
    }
}
