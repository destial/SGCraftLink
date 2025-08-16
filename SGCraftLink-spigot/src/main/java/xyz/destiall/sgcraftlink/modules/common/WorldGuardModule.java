package xyz.destiall.sgcraftlink.modules.common;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

public class WorldGuardModule extends Module {
    public WorldGuardModule(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    public RegionManager getRegionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
    }

    public ProtectedRegion getRegion(String world, String regionName) {
        World w = plugin.getServer().getWorld(world);
        if (w == null)
            return null;

        RegionManager rm = getRegionManager(w);
        if (rm == null)
            return null;

        return rm.getRegion(regionName);
    }

    public boolean isInRegion(Player player, World world, String regionName) {
        return isInRegion(player.getLocation(), world, regionName);
    }

    public boolean isInRegion(Location location, World world, String regionName) {
        RegionManager manager = getRegionManager(world);
        if (manager == null)
            return false;

        ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return regions.getRegions().stream().anyMatch(r -> r.getId().equalsIgnoreCase(regionName));
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {}

    @Override
    public boolean isPersistent() {
        return true;
    }
}
