package xyz.destiall.sgcraftlink.modules.vanilla;

import com.spawnchunk.piggybank.config.Config;
import com.spawnchunk.piggybank.modules.Account;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class PiggyBankModule extends Module implements Listener {
    private final Map<String, Double> balanceCached = new ConcurrentHashMap<>();

    public PiggyBankModule(SGCraftLinkSpigot plugin) {
        super(plugin, "piggybank.yml");
        cache();

        plugin.getScheduler().runTaskTimerAsync(this::recacheOnline, 0L, 20L);
    }

    public void cache() {
        OfflinePlayer[] players = plugin.getServer().getOfflinePlayers();
        for (OfflinePlayer op : players) {
            if (op != null) {
                String key = op.getUniqueId().toString();
                double balance = Account.readBalance(op);
                balanceCached.put(key, balance);
            }
        }
    }

    public void recacheOnline() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String key = player.getUniqueId().toString();
            double balance = Account.readBalance(player);
            balanceCached.put(key, balance);
        }
    }

    public Map.Entry<String, Double> getBaltop(int index) {
        if (index < 0)
            return null;

        List<Map.Entry<String, Double>> top = balanceCached.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if (index >= top.size())
            return null;

        Map.Entry<String, Double> entry = top.get(index);
        Player player = plugin.getServer().getPlayer(UUID.fromString(entry.getKey()));
        if (player != null) {
            double bal = Account.readBalance(player);
            double old_bal = entry.getValue();
            balanceCached.put(entry.getKey(), bal);
            if (bal != old_bal) {
                return getBaltop(index);
            }
        }

        return entry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickUp(EntityPickupItemEvent e) {
        if (!config.getBoolean("enabled"))
            return;

        if (!(e.getEntity() instanceof Player))
            return;

        Player player = (Player) e.getEntity();
        ItemStack item = e.getItem().getItemStack();

        if (Config.currency_items.keySet().stream().noneMatch(i -> i.getType() == item.getType()))
            return;

        for (ItemStack inv : player.getInventory()) {
            if (inv == null)
                continue;

            if (Config.currency_items.keySet().stream().anyMatch(i -> i.getType() == item.getType()))
                return;
        }

        String message = color(config.getString("pickup-message"));
        if (config.getBoolean("use-title")) {
            String sub = color(config.getString("pickup-message-subtitle"));
            player.showTitle(Title.title(Component.text(message), Component.text(sub)));
            return;
        }

        player.sendMessage(message);
    }


    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("use-title", false);
        config.set("pickup-message", "&bYou should store your diamonds in /warp Bank");
        config.set("pickup-message-subtitle", "&b/warp Bank");
    }

    @Override
    public void disable() {}
}
