package xyz.destiall.sgcraftlink.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.potion.PotionEffectType;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BreakAnimationModule extends Module implements Listener {

    private final Map<UUID, Location> players;
    public BreakAnimationModule(SGCraftLinkSpigot plugin) {
        super(plugin);
        players = new ConcurrentHashMap<>();
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {
         /**plugin.getScheduler().runTaskTimer(() -> {
            Iterator<Map.Entry<UUID, Location>> it = players.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Location> entry = it.next();
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player == null) {
                    it.remove();
                    continue;
                }

                player.sendBlockDamage(entry.getValue(), 0f);
            }
        }, 1L, 1L); **/
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(BlockDamageEvent event) {
        if (!isEnabled())
            return;

        BlockBreakEvent test = new BlockBreakEvent(event.getBlock(), event.getPlayer());
        plugin.getServer().getPluginManager().callEvent(test);
        if (!test.isCancelled())
            return;

        players.put(event.getPlayer().getUniqueId(), event.getBlock().getLocation());
        event.getPlayer().sendPotionEffectChange(event.getPlayer(), PotionEffectType.MINING_FATIGUE.createEffect(-1, 60).withIcon(false).withAmbient(true).withParticles(false));
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAbort(BlockDamageAbortEvent event) {
        if (!isEnabled())
            return;

        if (players.remove(event.getPlayer().getUniqueId()) != null) {
            event.getPlayer().sendPotionEffectChangeRemove(event.getPlayer(), PotionEffectType.MINING_FATIGUE);
        }
    }
}
