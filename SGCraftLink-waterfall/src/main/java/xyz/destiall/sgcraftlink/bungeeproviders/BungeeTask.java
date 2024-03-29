package xyz.destiall.sgcraftlink.bungeeproviders;

import net.md_5.bungee.api.scheduler.ScheduledTask;
import xyz.destiall.sgcraftlink.providers.SchedulerProvider;
import xyz.destiall.sgcraftlink.providers.tasks.Task;

public class BungeeTask extends Task {
    private final ScheduledTask task;
    public BungeeTask(SchedulerProvider<?> scheduler, ScheduledTask task) {
        super(scheduler);
        this.task = task;
    }

    public ScheduledTask getTask() {
        return task;
    }
}
