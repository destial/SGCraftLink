package xyz.destiall.sgcraftlink.gamemode;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.Module;
import xyz.destiall.sgcraftlink.modules.hooks.EconomyHook;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class MoveIfGay extends Module implements Listener, CommandExecutor {
    private boolean currentlyActive = false;
    private final Set<String> players;
    public MoveIfGay(SGCraftLinkSpigot plugin) {
        super(plugin, "move-if-gay.yml");
        players = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", false);
        config.set("duration-in-seconds", 30);
        config.set("prize-pool", 100000d);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        //players.add(e.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        players.remove(e.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerArmSwingEvent e) {
        moved(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrouch(PlayerToggleSneakEvent e) {
        moved(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        moved(e.getPlayer());
    }

    private void moved(Player player) {
        if (!currentlyActive)
            return;

        if (players.remove(player.getName())) {
            player.sendMessage(color("&cYou have moved!"));
            plugin.getServer().broadcastMessage(color("&c" + plugin.getName() + " has moved and is out!"));
        }
    }

    public void start() {
        if (isEnabled() || currentlyActive)
            return;

        EconomyHook economy = plugin.getModule(EconomyHook.class);
        if (economy == null)
            return;

        plugin.getServer().broadcastMessage(color("&cEnglish or Spanish?"));
        plugin.getServer().broadcastMessage(color("&cWhoever moves, is gay, and the winner(s) at the end will get a prize pool of $" + config.getDouble("prize-pool")));
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            players.add(player.getName());
        }

        plugin.getScheduler().runTaskLater(() -> {
            currentlyActive = true;
            plugin.getServer().broadcastMessage(color("&aThe game has now started!"));
            plugin.getScheduler().runTaskLater(() -> {
                currentlyActive = false;
                int winnerSize = players.size();
                float reward = (float) (config.getDouble("prize-pool") / (float) winnerSize);
                Component component = Component
                        .text("The game is over! We have " + winnerSize + " winner(s)!")
                        .color(TextColor.color(ChatColor.AQUA.getColor().getRGB()))
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text(String.join(", ", players))));
                plugin.getServer().broadcast(component);
                for (String name : players) {
                    Player p = plugin.getServer().getPlayer(name);
                    if (p == null)
                        continue;

                    economy.deposit(p, reward);
                    p.sendMessage(color("&a + $" + reward));
                }
            }, 20L * config.getInt("duration-in-seconds", 30));
        }, 40L);

    }

    @Override
    public void disable() {}

    @Override
    public void enable() {
        plugin.getCommand("iymyg").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("start")) {
            sender.sendMessage(color("&cUsage: /" + s + " start"));
            return false;
        }

        start();
        return false;
    }
}
