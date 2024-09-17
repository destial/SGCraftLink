package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.skinsrestorer.api.event.SkinApplyEvent;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.skins.PerServerSkin;

public class BungeePerServerSkin extends PerServerSkin {
    public BungeePerServerSkin(PluginProvider plugin) {
        super(plugin);
    }

    @Override
    public void onApplySkin(SkinApplyEvent e) {

        ProxiedPlayer player = e.getPlayer(ProxiedPlayer.class);
        Server server = player.getServer();
        if (server == null)
            return;

        if (isWhitelisted(server.getInfo().getName())) {
            e.setCancelled(true);
        }
    }
}
