package xyz.destiall.sgcraftlink.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.LuckPermsModule;
import xyz.destiall.sgcraftlink.modules.Module;

public class PlayerListener extends Module implements Listener {
    public PlayerListener(SGCraftLinkSpigot plugin) {
        super(plugin, null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        LuckPermsModule lp = plugin.getModule(LuckPermsModule.class);
        if (lp == null)
            return;

        plugin.getScheduler().runTaskLater(() -> {
            lp.sendRanks(e.getPlayer());
            plugin.sendFirstJoin(e.getPlayer());
        }, e.getPlayer(), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (plugin.getConfig().getBoolean("keep-inv-pvp", true)) {
            Player killer = e.getPlayer().getKiller();
            if (killer != null) {
                e.setKeepInventory(true);
                e.setKeepLevel(true);
                e.getDrops().clear();
                e.setDroppedExp(0);
            }
        }
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}
}
