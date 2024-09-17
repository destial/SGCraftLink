package xyz.destiall.sgcraftlink.velocityproviders;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.skinsrestorer.api.event.SkinApplyEvent;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.skins.PerServerSkin;

public class VelocityPerServerSkin extends PerServerSkin {
    public VelocityPerServerSkin(PluginProvider plugin) {
        super(plugin);
    }

    @Override
    public void onApplySkin(SkinApplyEvent e) {
        Player player = e.getPlayer(Player.class);
        ServerConnection con = player.getCurrentServer().orElse(null);
        if (con == null)
            return;

        if (isWhitelisted(con.getServerInfo().getName())) {
            e.setCancelled(true);
        }
    }
}
