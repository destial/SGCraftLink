package xyz.destiall.sgcraftlink.modules.common;

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
import org.json.JSONArray;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.items.Redeemable;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class Redeemables extends Module implements Listener, CommandExecutor, TabExecutor {
    private List<Redeemable> redeemables = new ArrayList<>();
    private boolean dirty;

    public Redeemables(SGCraftLinkSpigot plugin) {
        super(plugin, "redeemables.yml");
        setCommandExecutor("redeemables");
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
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
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
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
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
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        patriarchItem.setItemMeta(meta);
        Redeemable patriarch = new Redeemable("patriarch", patriarchItem, Collections.singletonList("lp user {player} parent addtemp patriarch 1d"), true);
        redeemables.add(patriarch);

        for (Redeemable redeemable : redeemables) {
            config.set(redeemable.getName(), redeemable.toYaml(config));
        }
    }

    @Override
    public void disable() {
        if (dirty) {
            for (Redeemable redeemable : redeemables) {
                config.set(redeemable.getName(), redeemable.toYaml(config));
            }
            dirty = false;
        }
        redeemables.clear();
        unsetCommandExecutor("redeemables");
    }

    @Override
    public void enable() {
        if (redeemables == null) {
            redeemables = new ArrayList<>();
        }

        if (redeemables.isEmpty()) {
            for (String key : config.getKeys(false)) {
                Redeemable redeemable = new Redeemable(key, config.getConfigurationSection(key));
                redeemables.add(redeemable);
            }
        }

        log("Registered " + redeemables.size() + " redeemables " + new JSONArray(redeemables.stream().map(Redeemable::getName).collect(Collectors.toList())).toString().replace("\"", ""));
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
                e.getPlayer().sendTitle(color("&aYou have successfully redeemed " + redeemable.getName()), color("&aYou are welcome!"));
                break;
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cYou need to be a player to use this command!"));
            return false;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(color("&cUsage: /redeemables [item|create] [name]"));
            return false;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length > 1) {
                String name = args[1];
                ItemStack holding = player.getInventory().getItemInMainHand();
                if (holding.getType() == Material.AIR) {
                    player.sendMessage(color("&cPlease hold the item you want to make a redeemable!"));
                    return false;
                }

                holding = holding.clone();
                holding.setAmount(1);
                Redeemable redeemable = new Redeemable(name, holding, new ArrayList<>(), true);
                redeemables.add(redeemable);
                dirty = true;
                player.sendMessage(color("&aCreated redeemable " + name + ". Open 'plugins/SGCraftLink/redeemables.yml' to edit the commands."));
                return false;
            }

            player.sendMessage(color("&cUsage: /redeemables create [name]"));
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
