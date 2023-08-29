package xyz.destiall.sgcraftlink.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import xyz.destiall.sgcraftlink.providers.PluginProvider;
import xyz.destiall.sgcraftlink.proxy.listeners.PMListener;

public class VelocityPMListener extends PMListener {
    public VelocityPMListener(PluginProvider plugin) {
        super(plugin);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        onPluginMessage(e.getIdentifier().getId(), e.getData());
    }
}
