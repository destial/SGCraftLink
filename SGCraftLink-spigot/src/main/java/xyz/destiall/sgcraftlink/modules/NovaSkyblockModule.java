package xyz.destiall.sgcraftlink.modules;

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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.xenondevs.nova.api.Nova;
import xyz.xenondevs.nova.api.block.NovaBlock;
import xyz.xenondevs.nova.api.block.NovaBlockState;
import xyz.xenondevs.nova.api.item.NovaItem;
import xyz.xenondevs.nova.api.protection.ProtectionIntegration;

import java.util.Locale;

public class NovaSkyblockModule extends Module implements Listener {
    private static final String NOVA_PREFIX = "NOVA";
    private final NovaKeyParser parser;
    private final SkyblockProtectionIntegration prot;

    public NovaSkyblockModule(SGCraftLinkSpigot plugin) {
        super(plugin, null);

        parser = new NovaKeyParser();

        prot = new SkyblockProtectionIntegration(plugin);

        plugin.getScheduler().runTask(() -> {
            plugin.getLogger().info("Registered Nova Key Parser");
            SuperiorSkyblockAPI.getBlockValues().registerKeyParser(parser);
            plugin.getLogger().info("Registered Skyblock Nova Protection Integration");
            Nova.getNova().registerProtectionIntegration(prot);
        });
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {}

    private static final LazyReference<WorldRecordService> worldRecordService = new LazyReference<WorldRecordService>() {
        @Override
        protected WorldRecordService create() {
            return SuperiorSkyblockPlugin.getPlugin().getServices().getService(WorldRecordService.class);
        }
    };

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNovaBlockBreak(BlockBreakEvent e) {
        NovaBlockState customBlock = Nova.getNova().getBlockManager().getBlock(e.getBlock().getLocation());
        if (customBlock == null)
            return;

        Key blockKey = parser.getCustomKey(e.getBlock().getLocation());
        worldRecordService.get().recordBlockBreak(blockKey, e.getBlock().getLocation(), 1, WorldRecordFlags.SAVE_BLOCK_COUNT | WorldRecordFlags.DIRTY_CHUNKS | WorldRecordFlags.HANDLE_NEARBY_BLOCKS);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandGenerateBlock(IslandGenerateBlockEvent event) {
        if (!event.getBlock().getGlobalKey().equals(NOVA_PREFIX))
            return;

        String itemId = event.getBlock().getSubKey().toLowerCase(Locale.ENGLISH).replace("|", ":");

        NovaBlock customBlock = Nova.getNova().getBlockRegistry().getOrNull(itemId);
        if (customBlock == null) {
            event.setCancelled(true);
            return;
        }

        event.setPlaceBlock(false);
        Nova.getNova().getBlockManager().placeBlock(event.getLocation(), customBlock);
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
