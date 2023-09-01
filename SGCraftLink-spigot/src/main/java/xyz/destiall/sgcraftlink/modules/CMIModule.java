package xyz.destiall.sgcraftlink.modules;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;
import com.Zrips.CMI.events.CMIPlayerJailEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class CMIModule extends Module implements Listener, CommandExecutor {
    private final Map<Player, Long> afkTime = new ConcurrentHashMap<>();
    private final Map<Player, List<Long>> repetitveAfk = new ConcurrentHashMap<>();

    public CMIModule(SGCraftLinkSpigot plugin) {
        super(plugin, null);

        plugin.getCommand("sgcwake").setExecutor(this);
    }

    @Override
    public void loadDefaultConfig() {}

    @Override
    public void disable() {
        plugin.getCommand("sgcwake").setExecutor(null);
    }

    @EventHandler
    public void onJail(CMIPlayerJailEvent e) {
        String reason = e.getUser().getJailedReason();
        long until = e.getUser().getJailedUntil();
        long duration = e.getUser().getJailedForTime();

        plugin.sendJail(e.getUser().getPlayer(), reason, until, duration);
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
        repetitveAfk.putIfAbsent(e.getPlayer(), new ArrayList<>());
        List<Long> repetitive = repetitveAfk.get(e.getPlayer());
        repetitive.add(duration);

        if (repetitive.size() < 3)
            return;

        double average = repetitive.stream().mapToDouble(a -> a).average().getAsDouble();
        double longest = repetitive.stream().mapToDouble(a -> a).max().getAsDouble();
        double lowest = repetitive.stream().mapToDouble(a -> a).min().getAsDouble();

        if (longest - average <= 1000d && average - lowest <= 1000d) {
            plugin.dispatchCommand("spawn " + e.getPlayer().getName());
            e.getPlayer().sendMessage(color("&cYou were detected as auto anti-AFK!"));
        }

        repetitive.clear();
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

    public JailResult getJailInfo(Player player) {
        CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
        if (user == null)
            return null;

        return user.isJailed() ? new JailResult(user.getJailedReason(), user.getJailedUntil(), user.getJailedForTime()) : null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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

    public static class JailResult {
        public final String reason;
        public final long until;
        public final long duration;

        public JailResult(String reason, long until, long duration) {
            this.reason = reason;
            this.until = until;
            this.duration = duration;
        }
    }
}
