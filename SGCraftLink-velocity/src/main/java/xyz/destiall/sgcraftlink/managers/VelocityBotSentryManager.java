package xyz.destiall.sgcraftlink.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.proxy.botsentry.BotSentryManager;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityPlayer;

public class VelocityBotSentryManager extends BotSentryManager {
    public VelocityBotSentryManager(SGCraftLinkVelocity plugin) {
        super(plugin);
        plugin.getProxy().getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        if (!event.kickedDuringServerConnect())
            return;

        Component kickMessage = event.getServerKickReason().orElse(null);
        TextComponent text = getAsTextComponent(kickMessage);
        if (text == null)
            return;

        if (text.content().toLowerCase().contains("blacklist")) {
            onIpBlacklisted(new VelocityPlayer(event.getPlayer()));
        }
    }

    private TextComponent getAsTextComponent(Component parent) {
        if (parent instanceof TextComponent) {
            return (TextComponent) parent;
        } else {
            for (Component children : parent.children()) {
                TextComponent text = getAsTextComponent(children);
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }
}
