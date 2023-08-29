package xyz.destiall.sgcraftlink.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.proxy.listeners.CommandLoggerListener;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityPlayer;

public class VelocityCommandListener extends CommandLoggerListener {
    public VelocityCommandListener(SGCraftLinkVelocity plugin) {
        super(plugin);
    }

    @Subscribe
    public void onChatEvent(PlayerChatEvent e) {
        String message = e.getMessage();
        if (!message.startsWith("/"))
            return;

        if (parseCommand(new VelocityPlayer(e.getPlayer()), message, false)) {
            e.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }

    @Subscribe
    public void onTabEvent(TabCompleteEvent e) {
        e.getSuggestions().removeIf(s -> parseCommand(new VelocityPlayer(e.getPlayer()), s, true));
    }
}
