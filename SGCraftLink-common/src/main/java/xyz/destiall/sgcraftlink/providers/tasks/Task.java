package xyz.destiall.sgcraftlink.providers.tasks;

import xyz.destiall.sgcraftlink.providers.SchedulerProvider;

public abstract class Task {
    private final SchedulerProvider<?> scheduler;

    public Task(SchedulerProvider<?> scheduler) {
        this.scheduler = scheduler;
    }

    public void cancel() {
        this.scheduler.cancel(this);
    }

}
