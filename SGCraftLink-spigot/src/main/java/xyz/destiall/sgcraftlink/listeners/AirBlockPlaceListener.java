package xyz.destiall.sgcraftlink.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class AirBlockPlaceListener extends Module implements Listener {
    private final BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public AirBlockPlaceListener(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Block against = e.getBlockAgainst();
        Block block = e.getBlockPlaced();

        /// CMI
        Location location = block.getLocation();
        if (!block.getWorld().getNearbyEntities(location, 0.5, 0.5, 0.5, f -> f.getType() == EntityType.ARMOR_STAND || f.getType() == EntityType.PAINTING).isEmpty()) {
            return;
        }

        if (against.getType() == Material.AIR) {
            e.setCancelled(true);
            return;
        }

        if (!block.getType().isBlock()) {
            return;
        }

        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (relative.getType() != Material.AIR)
                return;
        }

        e.setCancelled(true);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    @Override
    public void enable() {}
}
