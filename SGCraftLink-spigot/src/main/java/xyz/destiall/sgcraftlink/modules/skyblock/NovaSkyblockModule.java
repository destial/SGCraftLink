package xyz.destiall.sgcraftlink.modules.skyblock;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.events.IslandGenerateBlockEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.key.CustomKeyParser;
import com.bgsoftware.superiorskyblock.api.key.Key;
import com.bgsoftware.superiorskyblock.api.service.world.WorldRecordFlags;
import com.bgsoftware.superiorskyblock.api.service.world.WorldRecordService;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.core.LazyReference;
import com.bgsoftware.superiorskyblock.island.privilege.IslandPrivileges;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.xenondevs.nova.api.Nova;
import xyz.xenondevs.nova.api.block.NovaBlock;
import xyz.xenondevs.nova.api.block.NovaBlockState;
import xyz.xenondevs.nova.api.item.NovaItem;
import xyz.xenondevs.nova.api.protection.ProtectionIntegration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NovaSkyblockModule extends Module implements Listener {
    private static final String NOVA_PREFIX = "NOVA";
    private final NovaKeyParser parser;
    private final SkyblockProtectionIntegration prot;
    private final Set<Location> generatedBlocks;

    public NovaSkyblockModule(SGCraftLinkSpigot plugin) {
        super(plugin, "nova-skyblock.yml");

        parser = new NovaKeyParser();
        prot = new SkyblockProtectionIntegration(plugin);
        generatedBlocks = ConcurrentHashMap.newKeySet();
        if (config != null) {
            List<?> list = config.getList("generated-blocks", new ArrayList<>());
            for (Object o : list) {
                Location loc = (Location) o;
                generatedBlocks.add(loc);
            }
        }
        plugin.getScheduler().runTask(() -> {
            plugin.getLogger().info("Registered Nova Key Parser");
            SuperiorSkyblockAPI.getBlockValues().registerKeyParser(parser);
            plugin.getLogger().info("Registered Skyblock Nova Protection Integration");
            Nova.getNova().registerProtectionIntegration(prot);
        });

        plugin.getScheduler().runTaskTimer(() -> {
            Set<Location> removal = new HashSet<>();
            for (Location loc : generatedBlocks) {
                NovaBlockState customBlock = Nova.getNova().getBlockManager().getBlock(loc);
                if (customBlock == null) {
                    removal.add(loc);
                    Block block = loc.getBlock();
                    if (block.getType() == Material.AIR) {
                        plugin.getScheduler().runTaskLater(() -> {
                            if (block.getType() == Material.AIR) {
                                block.setType(Material.VOID_AIR, true);
                                if (block.getType() == Material.VOID_AIR) {
                                    plugin.getScheduler().runTaskLater(() -> {
                                        block.setType(Material.AIR, true);
                                    }, block.getLocation(), 1L);
                                }
                            }
                        }, block.getLocation(), 1L);
                    }
                }
            }

            generatedBlocks.removeIf(removal::contains);
        }, 5L, 2L);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {
        if (generatedBlocks != null) {
            List<Location> blocks = new ArrayList<>(generatedBlocks);
            config.set("generated-blocks", blocks);
        }
        save();
    }

    private static final LazyReference<WorldRecordService> worldRecordService = new LazyReference<WorldRecordService>() {
        @Override
        protected WorldRecordService create() {
            return SuperiorSkyblockPlugin.getPlugin().getServices().getService(WorldRecordService.class);
        }
    };

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent e) {
        plugin.getScheduler().runTaskLater(() -> {
            e.getPlayer().setAllowFlight(false);
            e.getPlayer().setFlying(false);
        }, e.getPlayer(), 5L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNovaBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();

        NovaBlockState customBlock = Nova.getNova().getBlockManager().getBlock(block.getLocation());
        if (customBlock == null)
            return;

        generatedBlocks.remove(block.getLocation());
        plugin.getScheduler().runTaskLater(() -> {
            block.setType(Material.VOID_AIR, true);
            plugin.getScheduler().runTaskLater(() -> {
                block.setType(Material.AIR, true);
            }, block.getLocation(), 1L);
        }, block.getLocation(), 1L);

        Key blockKey = parser.getCustomKey(block.getLocation());
        worldRecordService.get().recordBlockBreak(blockKey, block.getLocation(), 1, WorldRecordFlags.SAVE_BLOCK_COUNT | WorldRecordFlags.DIRTY_CHUNKS | WorldRecordFlags.HANDLE_NEARBY_BLOCKS);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandGenerateBlock(IslandGenerateBlockEvent e) {
        if (!e.getBlock().getGlobalKey().equals(NOVA_PREFIX))
            return;

        String itemId = e.getBlock().getSubKey().toLowerCase().replace("|", ":");
        NovaBlock customBlock = Nova.getNova().getBlockRegistry().getOrNull(itemId);
        if (customBlock == null) {
            e.setCancelled(true);
            return;
        }

        e.setPlaceBlock(false);
        Nova.getNova().getBlockManager().placeBlock(e.getLocation(), customBlock);
        generatedBlocks.add(e.getLocation());
    }

    private static class SkyblockProtectionIntegration implements ProtectionIntegration {
        private final SGCraftLinkSpigot plugin;

        private SkyblockProtectionIntegration(SGCraftLinkSpigot plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NotNull ExecutionMode getExecutionMode() {
            return plugin.getScheduler().isFolia() ? ExecutionMode.ASYNC : ExecutionMode.SERVER;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof SkyblockProtectionIntegration;
        }

        @Override
        public boolean canBreak(@NotNull OfflinePlayer offlinePlayer, @Nullable ItemStack itemStack, @NotNull Location location) {
            Island island = SuperiorSkyblockAPI.getIslandAt(location);
            if (island == null)
                return true;

            SuperiorPlayer player = SuperiorSkyblockAPI.getPlayer(offlinePlayer.getUniqueId());
            if (player == null)
                return false;

            return island.hasPermission(player, IslandPrivileges.BREAK);
        }

        @Override
        public boolean canPlace(@NotNull OfflinePlayer offlinePlayer, @NotNull ItemStack itemStack, @NotNull Location location) {
            Island island = SuperiorSkyblockAPI.getIslandAt(location);
            if (island == null)
                return true;

            SuperiorPlayer player = SuperiorSkyblockAPI.getPlayer(offlinePlayer.getUniqueId());
            if (player == null)
                return false;

            return island.hasPermission(player, IslandPrivileges.BUILD);
        }


        @Override
        public boolean canUseBlock(@NotNull OfflinePlayer offlinePlayer, @Nullable ItemStack itemStack, @NotNull Location location) {
            Island island = SuperiorSkyblockAPI.getIslandAt(location);
            if (island == null)
                return true;

            SuperiorPlayer player = SuperiorSkyblockAPI.getPlayer(offlinePlayer.getUniqueId());
            if (player == null)
                return false;

            return island.hasPermission(player, IslandPrivileges.CHEST_ACCESS);
        }

        @Override
        public boolean canUseItem(@NotNull OfflinePlayer offlinePlayer, @NotNull ItemStack itemStack, @NotNull Location location) {
            Island island = SuperiorSkyblockAPI.getIslandAt(location);
            if (island == null)
                return true;

            SuperiorPlayer player = SuperiorSkyblockAPI.getPlayer(offlinePlayer.getUniqueId());
            if (player == null)
                return false;

            return island.hasPermission(player, IslandPrivileges.CHEST_ACCESS);
        }

        @Override
        public boolean canInteractWithEntity(@NotNull OfflinePlayer offlinePlayer, @NotNull Entity entity, @Nullable ItemStack itemStack) {
            Island island = SuperiorSkyblockAPI.getIslandAt(entity.getLocation());
            if (island == null)
                return true;

            SuperiorPlayer player = SuperiorSkyblockAPI.getPlayer(offlinePlayer.getUniqueId());
            if (player == null)
                return false;

            return island.hasPermission(player, IslandPrivileges.CHEST_ACCESS);
        }

        @Override
        public boolean canHurtEntity(@NotNull OfflinePlayer offlinePlayer, @NotNull Entity entity, @Nullable ItemStack itemStack) {
            Island island = SuperiorSkyblockAPI.getIslandAt(entity.getLocation());
            if (island == null)
                return true;

            SuperiorPlayer player = SuperiorSkyblockAPI.getPlayer(offlinePlayer.getUniqueId());
            if (player == null)
                return false;

            return island.hasPermission(player, IslandPrivileges.ANIMAL_DAMAGE);
        }
    }

    private static class NovaKeyParser implements CustomKeyParser {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof NovaKeyParser;
        }

        @Override
        public Key getCustomKey(Location location) {
            NovaBlockState state = Nova.getNova().getBlockManager().getBlock(location);
            if (state == null)
                return Key.of(location.getBlock());
            return Key.of(NOVA_PREFIX, state.getBlock().getId().toNamespacedKey().asString().toUpperCase().replace(":", "|"));
        }

        @Override
        public Key getCustomKey(ItemStack itemStack, Key def) {
            NovaItem item = Nova.getNova().getItemRegistry().getOrNull(itemStack);
            if (item == null)
                return def;
            return Key.of(NOVA_PREFIX, item.getId().toNamespacedKey().asString().toUpperCase().replace(":", "|"));
        }

        @Override
        public boolean isCustomKey(Key key) {
            return key.getGlobalKey().equals(NOVA_PREFIX);
        }

        @Override
        public ItemStack getCustomKeyItem(Key key) {
            NovaItem item = Nova.getNova().getItemRegistry().getOrNull(key.getSubKey().toLowerCase().replace("|", ":"));
            if (item == null)
                return null;
            return item.createItemStack();
        }
    }
}
