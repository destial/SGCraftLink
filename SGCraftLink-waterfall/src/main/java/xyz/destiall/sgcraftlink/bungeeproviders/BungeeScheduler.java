package xyz.destiall.sgcraftlink.bungeeproviders;

import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.providers.SchedulerProvider;

import java.util.concurrent.TimeUnit;

public class BungeeScheduler extends SchedulerProvider<SGCraftLinkWaterfall> {
    public BungeeScheduler(SGCraftLinkWaterfall plugin) {
        super(plugin);
    }

    @Override
    public void schedule(Runnable task, long delay, long period, TimeUnit unit) {
        plugin.getProxy().getScheduler().schedule(plugin, task, delay, period, unit);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        plugin.getProxy().getScheduler().schedule(plugin, task, delay, unit);
    }

    @Override
    public void cancelAll() {
        plugin.getProxy().getScheduler().cancel(plugin);
    }

    @Override
    public boolean isNotAccepting() {
        return plugin.isShuttingDown();
    }
}
