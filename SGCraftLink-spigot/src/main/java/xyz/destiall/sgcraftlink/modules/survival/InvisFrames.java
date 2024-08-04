package xyz.destiall.sgcraftlink.modules.survival;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Collections;
import java.util.List;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class InvisFrames extends Module implements Listener, CommandExecutor, TabExecutor {
    private static NamespacedKey invisibleKey;

    private Material glowFrame = null;
    private EntityType glowFrameEntity = null;

    public InvisFrames(SGCraftLinkSpigot plugin) {
        super(plugin, "invis-frames.yml");
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
    }

    public static NamespacedKey getInvisibleKey() {
        return invisibleKey;
    }

    private boolean isFrameEntity(Entity entity) {
        return (entity != null && (entity.getType() == EntityType.ITEM_FRAME || entity.getType() == glowFrameEntity));
    }

    public static ItemStack generateInvisibleItemFrame() {
        ItemStack item = new ItemStack(Material.ITEM_FRAME, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Invisible Item Frame");
        meta.getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
        meta.setCustomModelData(11);
        item.setItemMeta(meta);
        return item;
    }

    //@EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (!isFrameEntity(event.getEntity()) || event.getPlayer() == null) {
            return;
        }

        // Get the frame item that the player placed
        ItemStack frame = event.getItemStack();
        Player p = event.getPlayer();
        if (frame == null) {
            if (p.getInventory().getItemInMainHand().getType() == Material.ITEM_FRAME || (glowFrame != null && p.getInventory().getItemInMainHand().getType() == glowFrame)) {
                frame = p.getInventory().getItemInMainHand();
            } else if (p.getInventory().getItemInOffHand().getType() == Material.ITEM_FRAME || (glowFrame != null && p.getInventory().getItemInOffHand().getType() == glowFrame)) {
                frame = p.getInventory().getItemInOffHand();
            } else {
                return;
            }
        }

        // If the frame item has the invisible tag, make the placed item frame invisible
        if (frame.getItemMeta().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE)) {
            ItemFrame itemFrame = (ItemFrame) event.getEntity();
            itemFrame.setVisible(false);
            if (frame.getType() == glowFrame) {
                itemFrame.setGlowing(true);
            }
            event.getEntity().getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    //@EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!isFrameEntity(event.getEntity()) || !event.getEntity().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
        ItemFrame itemFrame = (ItemFrame) event.getEntity();
        itemFrame.remove();

        if (event.getRemover() instanceof Player) {
            Player player = (Player) event.getRemover();
            if (player.getGameMode() == GameMode.CREATIVE)
                return;
        }

        dropItemFrame(itemFrame);
    }

    public void dropItemFrame(ItemFrame itemFrame) {
        ItemStack itemStack = generateInvisibleItemFrame();
        ItemMeta meta = itemStack.getItemMeta();
        if (glowFrame != null && itemFrame.getType() == glowFrameEntity) {
            meta.setDisplayName(ChatColor.WHITE + "Glow Invisible Item Frame");
            meta.getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
            meta.setCustomModelData(12);
            itemStack.setType(glowFrame);
            itemStack.setItemMeta(meta);
        } else {
            meta.getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
            meta.setCustomModelData(11);
            itemStack.setItemMeta(meta);
        }

        itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), itemStack);

        ItemStack inside = itemFrame.getItem();
        if (inside.getType() != Material.AIR) {
            itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), inside);
        }
    }

    //@EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakEvent event) {
        if (!isFrameEntity(event.getEntity()) || !event.getEntity().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
        ItemFrame itemFrame = (ItemFrame) event.getEntity();
        itemFrame.remove();

        dropItemFrame(itemFrame);
    }

    @Override
    public void disable() {
        plugin.getCommand("invisframes").setExecutor(null);
    }

    @Override
    public void enable() {
        invisibleKey = new NamespacedKey(plugin, "invisible");
        try {
            glowFrame = Material.valueOf("GLOW_ITEM_FRAME");
            glowFrameEntity = EntityType.valueOf("GLOW_ITEM_FRAME");
        } catch(IllegalArgumentException ignored) {}

        plugin.getCommand("invisframes").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player))
            return false;

        Player player = (Player) sender;
        if (args.length == 0) {
            ItemStack frame = generateInvisibleItemFrame();
            player.getInventory().addItem(frame);
            player.sendMessage(color("&aReceived an invisible item frame"));
            return false;
        }

        if (args[0].equalsIgnoreCase("glow")) {
            if (glowFrame != null) {
                ItemStack frame = generateInvisibleItemFrame();
                ItemMeta meta = frame.getItemMeta();
                meta.setDisplayName(ChatColor.WHITE + "Glow Invisible Item Frame");
                meta.getPersistentDataContainer().set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
                meta.setCustomModelData(12);
                frame.setType(glowFrame);
                frame.setItemMeta(meta);
                player.getInventory().addItem(frame);
                player.sendMessage(color("&aReceived an invisible glow item frame"));
                return false;
            }
            player.sendMessage(color("&cThis server doesn't have glowing item frames!"));
            return false;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getPermission() != null && !sender.hasPermission(command.getPermission()))
            return Collections.emptyList();

        return Collections.singletonList("glow");
    }
}
