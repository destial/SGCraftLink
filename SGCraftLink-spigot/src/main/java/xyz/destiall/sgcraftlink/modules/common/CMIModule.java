package xyz.destiall.sgcraftlink.modules.common;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;
import com.Zrips.CMI.events.CMIPlayerJailEvent;
import com.Zrips.CMI.events.CMIPlayerUnjailEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkCommon;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.modules.DataModule;
import xyz.destiall.sgcraftlink.modules.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.destiall.sgcraftlink.SGCraftLinkCommon.createDataMap;
import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class CMIModule extends Module implements Listener, CommandExecutor {
    private final Map<Player, Long> afkTime = new ConcurrentHashMap<>();
    private final Map<Player, List<Long>> repetitiveAfk = new ConcurrentHashMap<>();

    public CMIModule(SGCraftLinkSpigot plugin) {
        super(plugin);

        setCommandExecutor("sgcwake");
    }

    @Override
    public void disable() {
        unsetCommandExecutor("sgcwake");
    }

    @Override
    public void enable() {
    }

    @EventHandler
    public void onJail(CMIPlayerJailEvent e) {
        DataModule module = plugin.getModule(DataModule.class);
        if (module == null)
            return;

        plugin.getScheduler().runTaskLater(() -> {
            String reason = e.getUser().getJailedReason();
            long until = e.getUser().getJailedUntil();
            long duration = e.getUser().getJailedForTime();
            CMIUser ex = CMI.getInstance().getPlayerManager().getUser(e.getUser().getJailedBy());
            String executor = ex != null ? ex.getName() : "Server";

            Map<String, Object> json = createDataMap(
                    "username", e.getUser().getName(),
                    "executor", executor,
                    "reason", reason,
                    "until", until,
                    "duration", duration,
                    "jailed", true
                    );

            module.sendProxyData(SGCraftLinkCommon.DATA_JAILED, json);
        }, e.getUser().getPlayer(), 2L);
    }

    @EventHandler
    public void onUnjail(CMIPlayerUnjailEvent e) {
        if (!e.getUser().isJailed())
            return;

        DataModule module = plugin.getModule(DataModule.class);
        if (module == null)
            return;

        String reason = e.getUser().getJailedReason();
        long until = e.getUser().getJailedUntil();
        long duration = e.getUser().getJailedForTime();
        String executor;
        CMIUser jailer = CMI.getInstance().getPlayerManager().getUser(e.getUser().getJailedBy());
        if (jailer != null) {
            executor = jailer.getName();
        } else {
            executor = Bukkit.getOfflinePlayer(e.getUser().getJailedBy()).getName();
        }

        Map<String, Object> json = createDataMap(
                "username", e.getUser().getName(),
                "executor", executor,
                "reason", reason,
                "until", until,
                "duration", duration,
                "jailed", false
        );

        module.sendProxyData(SGCraftLinkCommon.DATA_JAILED, json);
    }

    @EventHandler
    public void onAFKEnter(CMIAfkEnterEvent e) {
        afkTime.put(e.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler
    public void onAFKLeave(CMIAfkLeaveEvent e) {
        long afkEnterTime = afkTime.getOrDefault(e.getPlayer(), Long.MIN_VALUE);
        if (afkEnterTime == Long.MIN_VALUE)
            return;

        long duration = System.currentTimeMillis() - afkEnterTime;
        repetitiveAfk.putIfAbsent(e.getPlayer(), new ArrayList<>());
        List<Long> repetitive = repetitiveAfk.get(e.getPlayer());
        repetitive.add(duration);

        if (repetitive.size() < 3)
            return;

        double average = repetitive.stream().mapToDouble(a -> a).average().getAsDouble();
        double longest = repetitive.stream().mapToDouble(a -> a).max().getAsDouble();
        double lowest = repetitive.stream().mapToDouble(a -> a).min().getAsDouble();

        if (longest - average <= 1000d && average - lowest <= 1000d) {
            plugin.dispatchCommand("spawn " + e.getPlayer().getName());
            e.getPlayer().sendMessage(color("&cYou were detected as auto anti-AFK!"));
            repetitiveAfk.remove(e.getPlayer());
        }

        repetitive.clear();
        afkTime.remove(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedLeave(PlayerBedLeaveEvent e) {
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(e.getPlayer());
        user.setAfk(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedLeave(PlayerBedEnterEvent e) {
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(e.getPlayer());
        user.setAfk(false);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(color("&cThis feature is not enabled!"));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(color("&cUsage: /sgcwake [player]"));
            return false;
        }
        Player player = plugin.getServer().getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(color("&cPlayer not found!"));
            return false;
        }

        try {
            player.wakeup(false);
            sender.sendMessage(color("&aWoke player up"));
        } catch (Exception e) {
            sender.sendMessage(color("&cPlayer is not sleeping!"));
        }

        return true;
    }
}
