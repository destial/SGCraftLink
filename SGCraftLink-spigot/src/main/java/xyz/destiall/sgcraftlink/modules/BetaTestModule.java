package xyz.destiall.sgcraftlink.modules;

import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import xyz.destiall.sgcraftlink.SGCraftLinkSpigot;
import xyz.destiall.sgcraftlink.hooks.DiscordSRVHook;

import static xyz.destiall.sgcraftlink.SGCraftLinkSpigot.color;

public class BetaTestModule extends Module {
    private BetaTestListener listener;

    public BetaTestModule(SGCraftLinkSpigot plugin) {
        super(plugin, "beta-test.yml");

        if (!isEnabled())
            return;

        listener = new BetaTestListener();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void loadDefaultConfig() {
        config.set("enabled", true);
        config.set("log-channel", "1181570472769495061");
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled");
    }

    @Override
    public void disable() {
        if (!isEnabled())
            return;

        HandlerList.unregisterAll(listener);
    }

    class BetaTestListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onCommand(PlayerCommandPreprocessEvent e) {
            if (e.getPlayer().hasPermission("betatest.admin"))
                return;

            log(e.getPlayer(), "executed command " + e.getMessage());
        }

        public void log(Player sender, String logMessage) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission("betatest.admin")) {
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
