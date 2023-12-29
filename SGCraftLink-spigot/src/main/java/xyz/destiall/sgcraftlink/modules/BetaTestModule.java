package xyz.destiall.sgcraftlink.modules;

import com.bgsoftware.superiorskyblock.api.events.PlayerToggleFlyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.hooks.DiscordSRVHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class BetaTestModule extends Module implements CommandExecutor, TabExecutor {
    private BetaTestListener listener;
    private Map<String, Set<String>> blacklist;
    private Set<String> eventNames;
    private boolean dirty = false;

    public BetaTestModule(SGCraftLinkSpigot plugin) {
        super(plugin, "beta-test.yml");

        if (!config.getBoolean("enabled"))
            return;

        listener = new BetaTestListener();
        blacklist = new HashMap<>();
        eventNames = ConcurrentHashMap.newKeySet();

        if (config.contains("blacklist")) {
            ConfigurationSection players = config.getConfigurationSection("blacklist");
            if (players != null) {
                for (String name : players.getKeys(false)) {
                    blacklist.putIfAbsent(name, new HashSet<>());
                    for (String type : players.getStringList(name)) {
                        blacklist.get(name).add(type);
                    }
                }
            }
        }

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getCommand("betatest").setExecutor(this);
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("log-channel", "1181570472769495061");
    }

    @Override
    public void save() {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : blacklist.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        config.set("blacklist", copy);
        super.save();
    }

    @Override
    public void disable() {
        if (!config.getBoolean("enabled"))
            return;

        if (dirty) {
            save();
            dirty = false;
        }
        HandlerList.unregisterAll(listener);
        plugin.getCommand("betatest").setExecutor(null);
        blacklist.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("betatest.admin")) {
            sender.sendMessage(color("&cYou do not have permission!"));
            return false;
        }

        // /betatest blacklist [event]
        if (args.length != 2) {
            sender.sendMessage(color("&cUsage: /betatest (remove/add)-event [event-type]"));
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "remove-event": {
                blacklist.putIfAbsent(sender.getName(), new HashSet<>());
                blacklist.get(sender.getName()).add(args[1]);
                dirty = true;
                sender.sendMessage(color("&aBlacklisted event " + args[1]));
                break;
            }
            case "add-event": {
                blacklist.putIfAbsent(sender.getName(), new HashSet<>());
                blacklist.get(sender.getName()).remove(args[1]);
                dirty = true;
                sender.sendMessage(color("&aWhitelisted event " + args[1]));
                break;
            }
            default:
                sender.sendMessage(color("&cUnknown option: " + args[0]));
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? Stream.of("add-event", "remove-event").filter(e -> e.startsWith(args[0].toLowerCase())).collect(Collectors.toList()) :
                args.length == 2 ? eventNames.stream().filter(e -> e.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList()) :
                        Collections.emptyList();
    }


    class BetaTestListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onCommand(PlayerCommandPreprocessEvent e) {
            log(e.getPlayer(), "executed command " + e.getMessage(), e.getClass());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onFly(PlayerToggleFlyEvent e) {
            log(e.getPlayer().asPlayer(), "toggled fly (SuperiorSkyblock event)", e.getClass());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onFlight(PlayerToggleFlightEvent e) {
            log(e.getPlayer(), "toggled flight " + e.isFlying(), e.getClass());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onFish(PlayerFishEvent e) {
            if (e.getCaught() != null)
                log(e.getPlayer(), "fished a(n) " + e.getCaught().getType().name(), e.getClass());
        }

        public void log(Player sender, String logMessage, Class<? extends Event> eventType) {
            eventNames.add(eventType.getSimpleName());
            if (sender.hasPermission("betatest.admin"))
                return;

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission("betatest.admin") && !blacklist.getOrDefault(player.getName(), Collections.emptySet()).contains(eventType.getSimpleName())) {
                    player.sendMessage(color("&e" + sender.getName() + ": " + logMessage));
                }
            }

            DiscordSRVHook hook = BetaTestModule.this.plugin.getModule(DiscordSRVHook.class);
            if (hook == null)
                return;

            TextChannel channel = hook.getTextChannel(config.getString("log-channel", "1181570472769495061"));
            if (channel == null)
                return;

            channel.sendMessage(sender.getName() + ": " + logMessage).queue();
        }
    }
}
