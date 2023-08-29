package xyz.destiall.sgcraftlink.listeners;

import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.proxy.listeners.PMListener;

public class BungeePMListener extends PMListener implements Listener {
    public BungeePMListener(SGCraftLinkWaterfall plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        onPluginMessage(e.getTag(), e.getData());
    }
}
