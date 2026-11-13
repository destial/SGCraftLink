package xyz.destiall.sgcraftlink.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.proxy.botsentry.BotSentryManager;
import xyz.destiall.sgcraftlink.velocityproviders.VelocityPlayer;

import java.util.HashSet;
import java.util.Set;

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
        if (kickMessage == null)
            return;

        Set<TextComponent> text = getAsTextComponent(kickMessage);
        if (text.isEmpty())
            return;

        for (TextComponent component : text) {
            if (component.content().toLowerCase().contains("blacklist")) {
                onIpBlacklisted(new VelocityPlayer(event.getPlayer()));
                return;
            }
        }
    }

    private Set<TextComponent> getAsTextComponent(Component parent) {
        Set<TextComponent> set = new HashSet<>();
        if (parent instanceof TextComponent) {
            set.add((TextComponent) parent);
        }
        for (Component children : parent.children()) {
            Set<TextComponent> deep = getAsTextComponent(children);
            set.addAll(deep);
        }
        return set;
    }
}
