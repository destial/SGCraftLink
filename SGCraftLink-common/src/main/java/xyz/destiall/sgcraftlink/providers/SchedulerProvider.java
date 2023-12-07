package xyz.destiall.sgcraftlink.providers;

import java.util.concurrent.TimeUnit;

public abstract class SchedulerProvider<T extends PluginProvider> {
    protected final T plugin;

    public SchedulerProvider(T plugin) {
        this.plugin = plugin;
    }

    public abstract void schedule(Runnable  task, long delay, long period, TimeUnit unit);
    public abstract void schedule(Runnable  task, long delay, TimeUnit unit);
    public abstract void cancelAll();
    public abstract boolean isNotAccepting();
}
