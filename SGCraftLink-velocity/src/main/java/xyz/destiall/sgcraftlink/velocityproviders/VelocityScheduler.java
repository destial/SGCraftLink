package xyz.destiall.sgcraftlink.velocityproviders;

import xyz.destiall.sgcraftlink.SGCraftLinkVelocity;
import xyz.destiall.sgcraftlink.providers.SchedulerProvider;
import xyz.destiall.sgcraftlink.providers.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VelocityScheduler extends SchedulerProvider<SGCraftLinkVelocity> {
    private final List<Task> tasks;
    public VelocityScheduler(SGCraftLinkVelocity plugin) {
        super(plugin);
        tasks = new ArrayList<>();
    }

    @Override
    public Task schedule(Runnable task, long delay, long period, TimeUnit unit) {
        Task t = new VelocityTask(this, plugin.getProxy().getScheduler().buildTask(plugin, task).delay(delay, unit).repeat(period, unit).schedule());
        tasks.add(t);
        return t;
    }

    @Override
    public Task schedule(Runnable task, long delay, TimeUnit unit) {
        Task t = new VelocityTask(this, plugin.getProxy().getScheduler().buildTask(plugin, task).delay(delay, unit).schedule());
        tasks.add(t);
        return t;
    }

    @Override
    public void cancelAll() {
        for (Task task : new ArrayList<>(tasks)) {
            try {
                ((VelocityTask) task).getTask().cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tasks.clear();
    }

    @Override
    public boolean cancel(Task task) {
        if (tasks.contains(task)) {
            ((VelocityTask) task).getTask().cancel();
            tasks.remove(task);
            return true;
        }
        return false;
    }

    @Override
    public boolean isNotAccepting() {
        return plugin.isShuttingDown();
    }
}
