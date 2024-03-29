package xyz.destiall.sgcraftlink.velocityproviders;

import com.velocitypowered.api.scheduler.ScheduledTask;
import xyz.destiall.sgcraftlink.providers.SchedulerProvider;
import xyz.destiall.sgcraftlink.providers.tasks.Task;

public class VelocityTask extends Task {
    private final ScheduledTask task;
    public VelocityTask(SchedulerProvider<?> scheduler, ScheduledTask task) {
        super(scheduler);
        this.task = task;
    }

    public ScheduledTask getTask() {
        return task;
    }
}
