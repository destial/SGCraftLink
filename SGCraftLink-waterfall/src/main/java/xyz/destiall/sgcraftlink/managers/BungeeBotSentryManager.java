package xyz.destiall.sgcraftlink.managers;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.bungeeproviders.BungeePlayer;
import xyz.destiall.sgcraftlink.proxy.botsentry.BotSentryManager;

public class BungeeBotSentryManager extends BotSentryManager implements Listener {
    public BungeeBotSentryManager(SGCraftLinkWaterfall plugin) {
        super(plugin);

        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @EventHandler
    public void onKick(ServerKickEvent e) {
        if (e.getState() != ServerKickEvent.State.CONNECTING)
            return;

        String reason = BaseComponent.toLegacyText(e.getReason());

        if (reason.toLowerCase().contains("blacklist")) {
            onIpBlacklisted(new BungeePlayer(e.getPlayer()));
        }
    }
}
