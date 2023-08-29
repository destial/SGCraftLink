package xyz.destiall.sgcraftlink.modules;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;
import com.Zrips.CMI.events.CMIPlayerJailEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.jetbrains.annotations.NotNull;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class CMIModule extends Module implements Listener, CommandExecutor {
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

    @EventHandler(ignoreCancelled = true)
    public void onBedLeave(PlayerBedLeaveEvent e) {
        CMIAfkLeaveEvent event = new CMIAfkLeaveEvent(e.getPlayer());
        plugin.getServer().getPluginManager().callEvent(event);
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
