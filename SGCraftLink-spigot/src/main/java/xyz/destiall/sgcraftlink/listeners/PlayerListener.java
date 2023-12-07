package xyz.destiall.sgcraftlink.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.LuckPermsModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.HashMap;
import java.util.Map;

public class PlayerListener extends Module implements Listener {
    public PlayerListener(SGCraftLinkSpigot plugin) {
        super(plugin, null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getScheduler().runTaskLater(() -> {
            DataModule data = plugin.getModule(DataModule.class);
            if (data != null) {
                Map<String, String> map = new HashMap<>();
                map.put("uuid", e.getPlayer().getUniqueId().toString());
                data.sendProxyData(SGCraftLinkCommon.DATA_SECURITY_CHECK, map, (result, content) -> {
                    if (result == 200) {
                        plugin.sendFirstJoin(e.getPlayer());
                        return;
                    }

                    if (e.getPlayer().isOnline()) {
                        e.getPlayer().kickPlayer("You are not joined via the proxy!");
                    }
                });
            }


        }, e.getPlayer(), 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        try {
            plugin.testPaperOr1_8Above();
        } catch (Exception ex) {
            return;
        }

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
