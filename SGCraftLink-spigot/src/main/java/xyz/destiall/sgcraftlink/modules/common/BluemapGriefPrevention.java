package xyz.destiall.sgcraftlink.modules.common;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.World;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Pair;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BluemapGriefPrevention extends Module {
    private BlueMapAPI bm;
    private GriefPrevention gp;

    public BluemapGriefPrevention(SGCraftLinkSpigot plugin) {
        super(plugin);
    }

    @Override
    public void disable() {}

    @Override
    public void enable() {
        plugin.getScheduler().runTaskLater(() -> {
            this.bm = BlueMapAPI.getInstance().orElse(null);
            this.gp = GriefPrevention.instance;
            if (this.bm == null) {
                error("Failed to find BlueMap instance!");
                return;
            }
            if (this.gp == null) {
                error("Failed to find GriefPrevention instance!");
                return;
            }

            plugin.getScheduler().runTaskTimer(this::syncClaims, 0, 300 * 20);
        }, 60L);
    }

    private void syncClaims() {
        Collection<Claim> claims = gp.dataStore.getClaims();
        log("Found " + claims.size() + " claims");
        log("Syncing claims...");

        Map<String, BlueMapWorld> blueMapWorlds = new HashMap<>();

        Collection<BlueMapWorld> worlds = bm.getWorlds();
        for (BlueMapWorld world : worlds) {
            String worldName = world.getId().split("#")[1];
            blueMapWorlds.put(worldName, world);
        }

        Map<String, Pair<World, List<Claim>>> claimsByWorld = new HashMap<>();

        for (Claim claim : claims) {
            World world = claim.getLesserBoundaryCorner().getWorld();

            if (world == null) continue;
            String name = world.getKey().toString();

            Pair<World, List<Claim>> claimsInWorld = claimsByWorld.computeIfAbsent(name, k -> new Pair<>(world, new ArrayList<>()));
            claimsInWorld.getValue().add(claim);
        }

        for (String worldName : blueMapWorlds.keySet()) {
            BlueMapWorld blueMapWorld = blueMapWorlds.get(worldName);
            Collection<BlueMapMap> maps = blueMapWorld.getMaps();

            MarkerSet claimsMarkerSet = MarkerSet.builder()
                    .label("Claims")
                    .build();

            if (claimsByWorld.get(worldName) != null) {
                Pair<World, List<Claim>> worldClaims = claimsByWorld.get(worldName);
                for (Claim claim : worldClaims.getValue()) {
                    int lowestY = worldClaims.getKey().getMinHeight();
                    int highestY = worldClaims.getKey().getMaxHeight();

                    Color outlineColor = getColorFromName(claim.getOwnerName());
                    Color mainColor = new Color((int) (outlineColor.getRed() * 0.9), (int) (outlineColor.getGreen() * 0.9), (int) (outlineColor.getBlue() * 0.9), 0.2f);

                    Marker claimMarker = ExtrudeMarker.builder()
                            .fillColor(mainColor)
                            .lineColor(outlineColor)
                            .lineWidth(2)
                            .shape(new Shape.Builder()
                                    .addPoint(new Vector2d(claim.getLesserBoundaryCorner().getX(), claim.getLesserBoundaryCorner().getZ()))
                                    .addPoint(new Vector2d(claim.getLesserBoundaryCorner().getX(), claim.getGreaterBoundaryCorner().getZ() + 1))
                                    .addPoint(new Vector2d(claim.getGreaterBoundaryCorner().getX() + 1, claim.getGreaterBoundaryCorner().getZ() + 1))
                                    .addPoint(new Vector2d(claim.getGreaterBoundaryCorner().getX() + 1, claim.getLesserBoundaryCorner().getZ()))
                                    .build(), (float) lowestY, (float) highestY)
                            .label(claim.getOwnerName() + "'s claim")
                            .build();


                    claimsMarkerSet.put(claim.getID().toString(), claimMarker);
                }
            }

            for (BlueMapMap map : maps) {
                map.getMarkerSets().put("claims", claimsMarkerSet);
            }
        }
    }

    private Color getColorFromName(String name) {
        Random random = new Random(name.hashCode());
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 1);
    }

}
