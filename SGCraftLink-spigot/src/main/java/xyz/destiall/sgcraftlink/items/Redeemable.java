package xyz.destiall.sgcraftlink.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.List;

public class Redeemable {
    private final String name;
    private final ItemStack item;
    private final boolean executeAsServer;
    private final List<String> commands;

    public Redeemable(String name, ItemStack item, List<String> commands, boolean executeAsServer) {
        this.name = name;
        this.item = item;
        this.commands = commands;
        this.executeAsServer = executeAsServer;
    }

    public Redeemable(String name, ConfigurationSection config) {
        this.name = name;
        this.item = config.getItemStack("item");
        this.commands = config.getStringList("commands");
        this.executeAsServer = config.getBoolean("execute-as-server");
    }

    public String getName() {
        return name;
    }

    public ItemStack getItem() {
        return item;
    }

    public ConfigurationSection toYaml(ConfigurationSection config) {
        ConfigurationSection section = config.createSection(name);
        section.set("item", item);
        section.set("commands", commands);
        section.set("execute-as-server", executeAsServer);
        return section;
    }

    public boolean redeem(Player player, ItemStack hand) {
        if (hand.isSimilar(item)) {
            hand.setAmount(hand.getAmount() - 1);

            for (String command : commands) {
                String cmd = command.replace("{player}", player.getName());

                if (executeAsServer) {
                    SGCraftLinkSpigot.getPlugin(SGCraftLinkSpigot.class).dispatchCommand(cmd, false);
                } else {
                    SGCraftLinkSpigot.getPlugin(SGCraftLinkSpigot.class).dispatchCommand(player, cmd, false);
                }
            }

            return true;
        }
        return false;
    }
}
