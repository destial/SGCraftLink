package xyz.destiall.sgcraftlink.modules.vanilla;

import org.bukkit.PortalType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class EndPortalModule extends Module implements Listener {
    public EndPortalModule(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {}

    @EventHandler
    public void onPortal(EntityPortalEvent e) {
        if (e.getEntity() instanceof Player) return;
        if (e.getPortalType() != PortalType.ENDER) return;
        e.setCancelled(true);
    }
}
