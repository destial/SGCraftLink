package xyz.destiall.sgcraftlink.velocityproviders;

import com.velocitypowered.api.scheduler.ScheduledTask;
import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.SchedulerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VelocityScheduler extends SchedulerProvider<SGCraftLinkVelocity> {
    private final List<ScheduledTask> tasks;
    public VelocityScheduler(SGCraftLinkVelocity plugin) {
        super(plugin);
        tasks = new ArrayList<>();
    }

    @Override
    public void schedule(Runnable task, long delay, long period, TimeUnit unit) {
        tasks.add(plugin.getProxy().getScheduler().buildTask(plugin, task).delay(delay, unit).repeat(period, unit).schedule());
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        tasks.add(plugin.getProxy().getScheduler().buildTask(plugin, task).delay(delay, unit).schedule());
    }

    @Override
    public void cancelAll() {
        for (ScheduledTask task : tasks) {
            try {
                task.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tasks.clear();
    }

    @Override
    public boolean isNotAccepting() {
        return plugin.isShuttingDown();
    }
}
