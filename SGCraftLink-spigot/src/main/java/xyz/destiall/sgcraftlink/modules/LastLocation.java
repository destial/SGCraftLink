package xyz.destiall.sgcraftlink.modules;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static me.clip.placeholderapi.util.Msg.color;

public class LastLocation extends Module implements CommandExecutor, Listener {
    private final Map<String, Location> lastLocations;
    private final Map<String, List<String>> pending;

    public LastLocation(SGCraftLinkSpigot plugin) {
        super(plugin, "last-locations.yml");
        lastLocations = new ConcurrentHashMap<>();
        pending = new HashMap<>();
        try {
            for (String name : config.getKeys(false)) {
                try {
                    Location loc = config.getLocation(name);
                    lastLocations.put(name, loc);
                } catch (Exception e) {
                    String w = config.getString(name + ".world");
                    if (w == null)
                        continue;
                    pending.putIfAbsent(w, new ArrayList<>());
                    pending.get(w).add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        plugin.getCommand("lastloc").setExecutor(this);
    }

    public void setLastLocation(Player player) {
        lastLocations.put(player.getName(), player.getLocation());

        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        config.set(player.getName(), lastLocations.get(player.getName()));
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {
        save();
        lastLocations.clear();
        pending.clear();

        plugin.getCommand("lastloc").setExecutor(null);
    }

    public void teleportLastLocation(Player player) {
        Location loc = lastLocations.get(player.getName());
        if (loc == null) return;
        try {
            player.teleportAsync(loc);
        } catch (Exception e) {
            player.teleport(loc);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastLocations.remove(e.getPlayer().getName());
        config.set(e.getPlayer().getName(), null);

        save();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        List<String> locs = pending.get(e.getWorld().getName());
        if (locs == null)
            return;

        if (config == null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
        for (String name : locs) {
            Location loc = config.getLocation(name);
            lastLocations.put(name, loc);
        }
        locs.clear();
        pending.remove(e.getWorld().getName());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 2) {
            sender.sendMessage(color("&cUsage: /lastloc [set/tp] [player]"));
            return false;
        }
        Player player = Bukkit.getPlayer(args[1]);
        if (player == null) {
            sender.sendMessage(color("&cPlayer not found!"));
            return false;
        }
        if (args[0].equalsIgnoreCase("set")) {
            setLastLocation(player);
        } else if (args[0].equalsIgnoreCase("tp")) {
            teleportLastLocation(player);
        }
        return true;
    }
}
