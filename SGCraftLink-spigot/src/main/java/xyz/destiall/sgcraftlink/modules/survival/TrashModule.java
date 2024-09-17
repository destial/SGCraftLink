package xyz.destiall.sgcraftlink.modules.survival;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.HashSet;
import java.util.Set;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class TrashModule extends Module implements CommandExecutor, Listener {
    private final Set<Inventory> inventories;
    public TrashModule(SGCraftLinkSpigot plugin) {
        super(plugin);
        setCommandExecutor("trash");
        inventories = new HashSet<>();
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {
        unsetCommandExecutor("trash");
    }

    @Override
    public void enable() {}

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent e) {
        if (inventories.remove(e.getInventory())) {
            e.getInventory().clear();
            e.getInventory().close();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command!"));
            return false;
        }
        Player player = (Player) sender;
        Inventory trashInventory = Bukkit.createInventory(null, 9 * 3, color("&cTrash"));
        inventories.add(trashInventory);
        player.openInventory(trashInventory);
        return false;
    }
}
