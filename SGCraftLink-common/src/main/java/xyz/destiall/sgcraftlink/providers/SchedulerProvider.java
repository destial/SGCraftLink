package xyz.destiall.sgcraftlink.providers;

import xyz.destiall.sgcraftlink.providers.tasks.Task;

import java.util.concurrent.TimeUnit;

public abstract class SchedulerProvider<T extends PluginProvider> {
    protected final T plugin;

    public SchedulerProvider(T plugin) {
        this.plugin = plugin;
    }

    public abstract Task schedule(Runnable task, long delay, long period, TimeUnit unit);
    public abstract Task schedule(Runnable task, long delay, TimeUnit unit);
    public abstract void cancelAll();
    public abstract boolean cancel(Task task);
    public abstract boolean isNotAccepting();
}
