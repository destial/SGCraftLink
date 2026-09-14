package xyz.destiall.sgcraftlink.modules.common;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.hooks.LuckPermsHook;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class SuffixesModule extends Module implements CommandExecutor, Listener {
    private final Map<UUID, Inventory> registeredInventories;
    private final NamespacedKey suffixKey;
    private final NamespacedKey pageKey;

    public SuffixesModule(SGCraftLinkSpigot plugin) {
        super(plugin);
        suffixKey = NamespacedKey.fromString("suffix", plugin);
        pageKey = NamespacedKey.fromString("page", plugin);
        registeredInventories = new ConcurrentHashMap<>();
    }

    @Override
    public void disable() {
        unsetCommandExecutor("suffixes");
        for (UUID uuid : registeredInventories.keySet()) {
            Player player = getPlugin().getServer().getPlayer(uuid);
            if (player == null)
                continue;

            player.closeInventory();
        }
        registeredInventories.clear();
    }

    @Override
    public void enable() {
        setCommandExecutor("suffixes");
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Inventory inv = fillInventory(player, 0);
            registeredInventories.put(player.getUniqueId(), inv);
            player.openInventory(inv);
        }
        return false;
    }

    ItemStack createSuffixItem(Player player, String suffix, boolean applied) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(suffix));
        meta.lore(Collections.singletonList(LegacyComponentSerializer.legacyAmpersand().deserialize("&r" + player.getName() + " " + suffix)));
        if (applied) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.getPersistentDataContainer().set(suffixKey, PersistentDataType.STRING, suffix);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack createPageItem(boolean next, int start) {
        ItemStack item = new ItemStack(Material.STONE_BUTTON, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize((next ? "Next" : "Previous") + " Page"));
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, start);
        item.setItemMeta(meta);
        return item;
    }

    ItemStack createClearItem() {
        ItemStack item = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize("Clear current suffix"));
        meta.getPersistentDataContainer().set(suffixKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    Inventory fillInventory(Player player, int start) {
        LuckPermsHook lp = getPlugin().getModule(LuckPermsHook.class);
        List<String> allSuffixes;
        if (lp == null) {
            allSuffixes = player.getEffectivePermissions().stream()
                    .filter(s -> s.getValue() && s.getPermission().toLowerCase().startsWith("sgc.suffixes."))
                    .map(s -> s.getPermission().substring("sgc.suffixes.".length()))
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            allSuffixes = lp.getPermissions(player).stream()
                    .filter(s -> s.toLowerCase().startsWith("sgc.suffixes."))
                    .map(s -> s.substring("sgc.suffixes.".length()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        String currentSuffix = allSuffixes.stream().filter(s -> s.toLowerCase().startsWith("suffix.1.")).findFirst().orElse(null);
        if (currentSuffix != null) {
            currentSuffix = currentSuffix.substring("suffix.1.".length());
        }

        Inventory inv = Bukkit.createInventory(null, 4 * 9, color("&1Suffixes Menu"));

        int finalI = 0;
        for (int i = 0; i < 26; i++) {
            finalI = i + start;
            if (finalI >= allSuffixes.size()) {
                continue;
            }

            String suffix = allSuffixes.get(finalI);
            inv.setItem(i, createSuffixItem(player, suffix, suffix.equalsIgnoreCase(currentSuffix)));
        }

        if (start != 0) {
            inv.setItem(27, createPageItem(false, start - 26));
        }

        if (allSuffixes.size() >= finalI) {
            inv.setItem(35, createPageItem(true, start + 26));
        }

        inv.setItem(31, createClearItem());

        return inv;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!registeredInventories.containsKey(e.getWhoClicked().getUniqueId()))  {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        e.setCancelled(true);
        //e.setResult(Event.Result.DENY);

        ItemStack item = e.getCurrentItem();
        if (item == null) {
            return;
        }

        if (item.getType() == Material.PAPER) {
            if (!item.getItemMeta().getPersistentDataContainer().has(suffixKey)) {
                return;
            }

            String suffix = item.getItemMeta().getPersistentDataContainer().get(suffixKey, PersistentDataType.STRING);
            plugin.dispatchCommand("lp user " + player.getName() + " meta setsuffix " + suffix);
            player.sendMessage(color("&aApplied " + suffix));
            plugin.getScheduler().runTaskLater(() -> {
                player.closeInventory();
                registeredInventories.remove(player.getUniqueId());
            }, player, 1L);
            return;
        }

        if (item.getType() == Material.BARRIER) {
            if (!item.getItemMeta().getPersistentDataContainer().has(suffixKey)) {
                return;
            }

            plugin.dispatchCommand("lp user " + player.getName() + " meta removesuffix 1");
            plugin.getScheduler().runTaskLater(() -> {
                player.closeInventory();
                registeredInventories.remove(player.getUniqueId());
            }, player, 1L);
            return;
        }

        if (item.getType() == Material.STONE_BUTTON) {
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(pageKey)) {
                return;
            }

            Integer start = pdc.get(pageKey, PersistentDataType.INTEGER);
            plugin.getScheduler().runTaskLater(() -> {
                player.closeInventory();
                registeredInventories.remove(player.getUniqueId());

                plugin.getScheduler().runTaskLater(() -> {
                    Inventory inv = fillInventory(player, start != null ? start : 0);
                    registeredInventories.put(player.getUniqueId(), inv);
                    player.openInventory(inv);
                }, player, 1L);
            }, player, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        registeredInventories.remove(e.getPlayer().getUniqueId());
    }
}
