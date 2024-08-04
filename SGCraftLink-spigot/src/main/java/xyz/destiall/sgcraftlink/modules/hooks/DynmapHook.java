package xyz.destiall.sgcraftlink.modules.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.GenericMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DynmapHook extends Module {
    MarkerSet set;
    AreaStyle areaStyle;
    List<ChunkYML> chunks;
    boolean fullyLoaded = false;
    public DynmapHook(SGCraftLinkSpigot plugin) {
        super(plugin, "dynmap.yml");
    }

    public void findChunks() {
        plugin.getScheduler().runTask(() -> {
            if (!fullyLoaded) {
                for (World world : plugin.getServer().getWorlds()) {
                    int size = (int) (world.getWorldBorder().getSize() / 2);
                    Location center = world.getWorldBorder().getCenter();
                    int offsetX = center.getBlockX();
                    int offsetZ = center.getBlockZ();

                    int minX = (-size + offsetX) >> 4;
                    int minZ = (-size + offsetZ) >> 4;

                    int maxX = (size + offsetX) >> 4;
                    int maxZ = (size + offsetZ) >> 4;

                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            final int fX = x;
                            final int fZ = z;
                            if (chunks.stream().anyMatch(c -> c.world.equalsIgnoreCase(world.getName()) && c.x == fX && c.z == fZ))
                                continue;

                            world.getChunkAtAsync(x, z).whenComplete((chunk, err) -> {
                                if (chunk.isSlimeChunk()) {
                                    ChunkYML yml = new ChunkYML(chunk);
                                    chunks.add(yml);
                                    plugin.getScheduler().runTask(() -> {
                                        String path = "slime-chunk-" + yml.world + "-" + yml.x + "-" + yml.z;
                                        String id = yml.world + "-" + yml.x + "-" + yml.z;
                                        double[] xs = new double[]{yml.x << 4, yml.x << 4 + 16};
                                        double[] zs = new double[]{yml.z << 4, yml.z << 4 + 16};
                                        AreaMarker areaMarker = set.findAreaMarker(path);
                                        if (areaMarker == null) {
                                            areaMarker = set.createAreaMarker(path, id, false, yml.world, xs, zs, false);
                                        }
                                        areaMarker.setLineStyle(areaStyle.getStrokeWeight(), areaStyle.getStrokeOpacity(), areaStyle.getStrokeColor());
                                        areaMarker.setFillStyle(areaStyle.getFillOpacity(), areaStyle.getFillColor());
                                    }, chunk);
                                }
                            });
                        }
                    }
                }
                fullyLoaded = true;
            }

            startDynmapRender();
        });
    }

    public void startDynmapRender() {
        for (ChunkYML chunkYML : chunks) {
            chunkYML.getChunk().whenComplete((chunk, err) -> {
                if (chunk == null) {
                    if (err != null)
                        err.printStackTrace();

                    return;
                }
                plugin.getScheduler().runTask(() -> {
                    String path = "slime-chunk-" + chunkYML.world + "-" + chunkYML.x + "-" + chunkYML.z;
                    String id = chunkYML.world + "-" + chunkYML.x + "-" + chunkYML.z;
                    double[] x = new double[]{chunkYML.x >> 4, chunkYML.x >> 4 + 16};
                    double[] z = new double[]{chunkYML.z >> 4, chunkYML.z >> 4 + 16};
                    AreaMarker areaMarker = set.findAreaMarker(path);
                    if (areaMarker == null) {
                        areaMarker = set.createAreaMarker(path, id, false, chunkYML.world, x, z, false);
                    }
                    areaMarker.setLineStyle(areaStyle.getStrokeWeight(), areaStyle.getStrokeOpacity(), areaStyle.getStrokeColor());
                    areaMarker.setFillStyle(areaStyle.getFillOpacity(), areaStyle.getFillColor());

                }, chunk);
            });
        }
    }

    @Override
    public void save() {
        if (chunks == null)
            return;
        int index = 0;
        for (ChunkYML chunkYML : chunks) {
            String path = "slime-chunks.chunks." + (index++);
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section == null) {
                section = config.createSection(path);
            }
            chunkYML.toYaml(section);
        }
        config.set("slime-chunks.fully-loaded", fullyLoaded);
        super.save();
    }

    @Override
    public void disable() {
        if (set == null)
            return;

        set.getMarkers().forEach(GenericMarker::deleteMarker);
        set.deleteMarkerSet();
    }

    @Override
    public void enable() {
        DynmapAPI api = (DynmapAPI) plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (api == null)
            return;

        if (!config.getBoolean("slime-chunks.enabled", false)) {
            return;
        }

        MarkerAPI marker = api.getMarkerAPI();
        set = marker.getMarkerSet("slime-chunks");

        if (set == null) {
            set = marker.createMarkerSet("slime-chunks", "Slime Chunks", null, true);
        }

        set.setHideByDefault(config.getBoolean("slime-chunks.hide-by-default", true));
        set.setMarkerSetLabel(config.getString("slime-chunks.label", "Slime Chunks"));
        areaStyle = new AreaStyle(config, "slime-chunks.area-style");
        fullyLoaded = config.getBoolean("slime-chunks.fully-loaded", false);

        ConfigurationSection chunkSection = config.getConfigurationSection("slime-chunks.chunks");
        if (chunkSection == null)
            return;

        for (String key : chunkSection.getKeys(false)) {
            ConfigurationSection section = chunkSection.getConfigurationSection(key);
            if (section == null)
                continue;

            ChunkYML chunkYML = new ChunkYML(section);
            chunks.add(chunkYML);
        }

        findChunks();
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("slime-chunks.enabled", false);
        config.set("slime-chunks.label", "Slime Chunks");
        config.set("slime-chunks.hide-by-default", true);
        config.set("slime-chunks.fully-loaded", false);
        config.set("slime-chunks.area-style.stroke-color", "00FF00");
        config.set("slime-chunks.area-style.stroke-opacity", 0.8);
        config.set("slime-chunks.area-style.stroke-weight", 3);
        config.set("slime-chunks.area-style.fill-color", "00FF00");
        config.set("slime-chunks.area-style.fill-opacity", 0.35);
    }

    private static class AreaStyle {
        private int strokecolor;
        private int fillcolor;

        private final double strokeopacity;
        private final int strokeweight;
        private final double fillopacity;

        AreaStyle(FileConfiguration cfg, String path) {
            String sc = cfg.getString(path + ".stroke-color", null);
            strokeopacity = cfg.getDouble(path + ".stroke-opacity", -1);
            strokeweight = cfg.getInt(path + ".stroke-weight", -1);
            String fc = cfg.getString(path + ".fill-color", null);

            strokecolor = -1;
            fillcolor = -1;
            try {
                if(sc != null)
                    strokecolor = Integer.parseInt(sc, 16);
                if(fc != null)
                    fillcolor = Integer.parseInt(fc, 16);
            } catch (NumberFormatException ignored) {}

            fillopacity = cfg.getDouble(path + ".fill-opacity", -1);
        }

        public int getStrokeColor() {
            return strokecolor >= 0 ? strokecolor : 0xFF0000;
        }

        public double getStrokeOpacity() {
            return strokeopacity >= 0 ? strokeopacity : 0.8;
        }

        public int getStrokeWeight() {
            return strokeweight >= 0 ? strokeweight : 3;
        }

        public int getFillColor() {
            return fillcolor >= 0 ? fillcolor : 0xFF0000;
        }

        public double getFillOpacity() {
            return fillopacity >= 0 ? fillopacity : 0.35;
        }
    }

    public static class ChunkYML {
        private final int x;
        private final int z;
        private final String world;

        public ChunkYML(Chunk chunk) {
            this.x = chunk.getX();
            this.z = chunk.getZ();
            this.world = chunk.getWorld().getName();
        }

        public ChunkYML(ConfigurationSection section) {
            this.world = section.getString("world");
            this.x = section.getInt("x");
            this.z = section.getInt("z");
        }

        public CompletableFuture<Chunk> getChunk() {
            return Bukkit.getWorld(world).getChunkAtAsync(x, z);
        }

        public void toYaml(ConfigurationSection section) {
            section.set("world", world);
            section.set("x", x);
            section.set("z", z);
        }
    }
}
