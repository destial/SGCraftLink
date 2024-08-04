package xyz.destiall.sgcraftlink.proxy.skins;

import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.event.SkinApplyEvent;
import xyz.destiall.sgcraftlink.providers.ConfigProvider;
import xyz.destiall.sgcraftlink.providers.PluginProvider;

import java.util.List;

public abstract class PerServerSkin {
    protected final PluginProvider plugin;
    protected final ConfigProvider<?> config;
    protected final List<String> whitelisted;

    public PerServerSkin(PluginProvider plugin) {
        this.plugin = plugin;
        config = plugin.getConfigProvider().createConfig("per-server-skins.yml");
        whitelisted = config.getConfig().getStringList("whitelisted");
        SkinsRestorerProvider.get().getEventBus().subscribe(plugin, SkinApplyEvent.class, this::onApplySkin);
    }

    protected boolean isWhitelisted(String serverName) {
        return whitelisted.stream().anyMatch(w -> w.equalsIgnoreCase(serverName));
    }

    public abstract void onApplySkin(SkinApplyEvent e);
}
