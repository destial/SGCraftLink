package xyz.destiall.sgcraftlink.bungeeproviders;

import xyz.destiall.sgcraftlink.SGCraftLinkWaterfall;
import xyz.destiall.sgcraftlink.providers.SchedulerProvider;
import xyz.destiall.sgcraftlink.providers.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BungeeScheduler extends SchedulerProvider<SGCraftLinkWaterfall> {
    private final List<Task> tasks;
    public BungeeScheduler(SGCraftLinkWaterfall plugin) {
        super(plugin);
        this.tasks = new ArrayList<>();
    }

    @Override
    public Task schedule(Runnable task, long delay, long period, TimeUnit unit) {
        Task t = new BungeeTask(this, plugin.getProxy().getScheduler().schedule(plugin, task, delay, period, unit));
        tasks.add(t);
        return t;
    }

    @Override
    public Task schedule(Runnable task, long delay, TimeUnit unit) {
        Task t = new BungeeTask(this, plugin.getProxy().getScheduler().schedule(plugin, task, delay, unit));
        tasks.add(t);
        return t;
    }

    @Override
    public void cancelAll() {
        for (Task task : new ArrayList<>(tasks)) {
            try {
                ((BungeeTask) task).getTask().cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tasks.clear();
        plugin.getProxy().getScheduler().cancel(plugin);
    }

    @Override
    public boolean cancel(Task task) {
        if (tasks.contains(task)) {
            ((BungeeTask) task).getTask().cancel();
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
