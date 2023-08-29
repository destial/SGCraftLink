package xyz.destiall.sgcraftlink.modules;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Redeemable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class Redeemables extends Module implements Listener, CommandExecutor, TabExecutor {
    private List<Redeemable> redeemables = new ArrayList<>();

    public Redeemables(SGCraftLinkSpigot plugin) {
        super(plugin, "redeemables.yml");
        plugin.getCommand("redeemables").setExecutor(this);

        if (redeemables == null) {
            redeemables = new ArrayList<>();
        }

        if (redeemables.isEmpty()) {
            for (String key : config.getKeys(false)) {
                Redeemable redeemable = new Redeemable(key, config.getConfigurationSection(key));
                redeemables.add(redeemable);
            }
        }
    }

    @Override
    public void loadDefaultConfig() {
        if (redeemables == null) {
            redeemables = new ArrayList<>();
        }
        ItemStack exarchItem = new ItemStack(Material.PAPER);
        ItemMeta meta = exarchItem.getItemMeta();
        meta.setDisplayName(color("&dRight-click to redeem"));
        meta.setLore(Arrays.asList(
                color("<#00EA47>ᴇxᴀʀᴄʜ"),
                color("&c1 Day")
        ));
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS);
        exarchItem.setItemMeta(meta);
        Redeemable exarch = new Redeemable("exarch", exarchItem, Collections.singletonList("lp user {player} parent addtemp exarch 1d"), true);
        redeemables.add(exarch);

        ItemStack bishopItem = new ItemStack(Material.PAPER);
        meta = bishopItem.getItemMeta();
        meta.setDisplayName(color("&dRight-click to redeem"));
        meta.setLore(Arrays.asList(
                color("<#00DADA>ʙɪꜱʜᴏᴘ"),
                color("&c1 Day")
        ));
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS);
        bishopItem.setItemMeta(meta);
        Redeemable bishop = new Redeemable("bishop", bishopItem, Collections.singletonList("lp user {player} parent addtemp bishop 1d"), true);
        redeemables.add(bishop);

        ItemStack patriarchItem = new ItemStack(Material.PAPER);
        meta = patriarchItem.getItemMeta();
        meta.setDisplayName(color("&dRight-click to redeem"));
        meta.setLore(Arrays.asList(
                color("<#CBD500>ᴘᴀᴛʀɪᴀʀᴄʜ"),
                color("&c1 Day")
        ));
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS);
        patriarchItem.setItemMeta(meta);
        Redeemable patriarch = new Redeemable("patriarch", patriarchItem, Collections.singletonList("lp user {player} parent addtemp patriarch 1d"), true);
        redeemables.add(patriarch);

        for (Redeemable redeemable : redeemables) {
            config.set(redeemable.getName(), redeemable.toYaml(config));
        }
    }

    @Override
    public void disable() {
        redeemables.clear();

        plugin.getCommand("redeemables").setExecutor(null);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = e.getItem();

        if (item == null || item.getType() == Material.AIR)
            return;

        for (Redeemable redeemable : redeemables) {
            if (redeemable.redeem(e.getPlayer(), item)) {
                e.getPlayer().sendMessage(color("&aYou have successfully redeemed " + redeemable.getName()));
                break;
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("redeemables.admin")) {
            sender.sendMessage(color("&cYou do not have permission!"));
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cYou need to be a player to use this command!"));
            return false;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(color("&cUsage: /redeemables [item]"));
            return false;
        }

        String name = args[0];
        for (Redeemable redeemable : redeemables) {
            if (redeemable.getName().equalsIgnoreCase(name)) {
                player.getInventory().addItem(redeemable.getItem().clone());
                player.sendMessage(color("&aYou have been given an " + redeemable.getName() + " redeemable"));
                break;
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("redeemables.admin") || args.length != 1)
            return Collections.emptyList();

        return redeemables.stream()
                .map(Redeemable::getName)
                .filter(r -> r.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
