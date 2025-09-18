package xyz.destiall.sgcraftlink.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {
    private final Set<TimerTask> tasks;
    private final Timer timer;

    public Scheduler() {
        tasks = new HashSet<>();
        timer = new Timer();
    }

    public TimerTask schedule(Runnable runnable, long delayInMillis) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }

            @Override
            public boolean cancel() {
                tasks.remove(this);
                return super.cancel();
            }
        };
        tasks.add(task);
        timer.schedule(task, delayInMillis);
        return task;
    }

    public void shutdown() {
        List<TimerTask> copied = new ArrayList<>(tasks);
        for (TimerTask task : copied) {
            task.cancel();
        }
        tasks.clear();
        timer.cancel();
    }
}
