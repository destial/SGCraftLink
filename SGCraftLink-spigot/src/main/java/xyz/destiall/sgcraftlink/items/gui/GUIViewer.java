package xyz.destiall.sgcraftlink.items.gui;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.survival.MythicItemAbilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIViewer implements Listener {
    private final SGCraftLinkSpigot plugin;
    private final int rows;
    private final Map<Integer, GUIItem> items;
    private final Map<UUID, Inventory> inventory;
    private final List<String> closeActions;

    public GUIViewer(SGCraftLinkSpigot plugin, ConfigurationSection section) {
        this.plugin = plugin;
        rows = section.getInt("rows", 4);
        closeActions = section.getStringList("on-close");
        items = new HashMap<>();
        inventory = new ConcurrentHashMap<>();

        // "fixed.0.item" "fixed.0.slots"
        ConfigurationSection fixed = section.getConfigurationSection("fixed");
        if (fixed != null) {
            for (String key : fixed.getKeys(false)) {
                List<String> slots = fixed.getStringList(key + ".slots");
                ItemStack itemStack = parseItem(section, "item");
                for (String slot : slots) {
                    int i = Integer.parseInt(slot);
                    GUIItem item = new GUIItem(this, i, itemStack);
                    items.put(i, item);
                }
            }
        }

        for (String key : section.getConfigurationSection("items").getKeys(false)) {
            int slot = Integer.parseInt(key);
            GUIItem item = new GUIItem(this, slot, section);
            items.put(slot, item);
        }
    }

    public ItemStack parseItem(ConfigurationSection section, String key) {
        ItemStack result;
        if (section.isItemStack(key)) {
            result = section.getItemStack(key + key);
        } else {
            result = parseItem(section.getString(key, null));
        }
        return result;
    }

    public ItemStack parseItem(String entry) {
        if (entry == null)
            return null;

        if (entry.startsWith("mythicmobs:")) {
            MythicItemAbilities mm = plugin.getModule(MythicItemAbilities.class);
            if (mm == null) {
                plugin.getLogger().severe("MythicMobs is not available! Unable to parse: " + entry);
                return null;
            }

            String name = entry.substring("mythicmobs:".length());
            int amount = 1;
            if (name.contains(":")) {
                amount = Integer.parseInt(name.substring(name.indexOf(":") + 1));
                name = name.substring(0, name.indexOf(":"));
            }

            MythicItem mmitem = mm.getMythic().getItemManager().getItem(name).orElse(null);

            if (mmitem == null) {
                plugin.getLogger().severe("Unable to find MythicMobs item: " + name);
                return null;
            }

            return BukkitAdapter.adapt(mmitem.generateItemStack(amount));
        }

        // add support for other items maybe

        try {
            int amount = 1;
            if (entry.contains(":")) {
                amount = Integer.parseInt(entry.substring(entry.indexOf(":")));
                entry = entry.substring(0, entry.indexOf(":"));
            }

            Material material = Material.matchMaterial(entry);
            if (material == null) {
                plugin.getLogger().severe("Unable to parse material: " + entry);
                return null;
            }

            ItemStack item = new ItemStack(material);
            item.setAmount(amount);
            return item;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void open(Player player) {
        if (inventory.containsKey(player.getUniqueId()))
            return;

        Inventory inv = Bukkit.createInventory(null, rows * 9);
        for (Map.Entry<Integer, GUIItem> entry : items.entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue().getItem());
        }

        inventory.put(player.getUniqueId(), inv);
    }

    public void close(Player player) {
        if (!inventory.containsKey(player.getUniqueId()))
            return;

        player.closeInventory();
        inventory.remove(player.getUniqueId());
        triggerCloseActions(player);
    }

    public Map<Integer, GUIItem> getItems() {
        return items;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!inventory.containsKey(e.getWhoClicked().getUniqueId()))
            return;

        Inventory inv = inventory.get(e.getWhoClicked().getUniqueId());
        if (inv.equals(e.getClickedInventory())) {
            GUIItem item = items.get(e.getSlot());
            if (item != null) {
                e.setCancelled(true);
                item.onClick(e, (Player) e.getWhoClicked());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!inventory.containsKey(e.getPlayer().getUniqueId()))
            return;

        inventory.remove(e.getPlayer().getUniqueId());
        triggerCloseActions((Player) e.getPlayer());
    }

    public void triggerCloseActions(Player player) {
        for (String action : closeActions) {

        }
    }
}
