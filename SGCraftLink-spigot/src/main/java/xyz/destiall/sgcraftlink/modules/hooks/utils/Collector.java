package xyz.destiall.sgcraftlink.modules.hooks.utils;

import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageChannel;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class Collector<C extends Collector<C, B, T, E>, B, T, E> {
    protected final JDA jda;
    protected final MessageChannel channel;

    protected boolean closed = true;

    protected long time = -1;
    protected int limit = -1;
    protected boolean dispose = true;

    protected Timer timer;
    protected ListenerAdapter listener;
    protected Predicate<E> filter;
    protected Consumer<T> collect;
    protected Consumer<List<T>> end;
    protected List<T> collected;

    public Collector(MessageChannel channel) {
        this.jda = channel.getJDA();
        this.channel = channel;
    }

    public C setFilter(Predicate<E> filter) {
        this.filter = filter;
        return (C) this;
    }

    public C setTime(long time) {
        this.time = time;
        if (!closed) {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            if (time != -1) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        stop();
                    }
                }, time);
            }
        }
        return (C) this;
    }

    public C setDispose(boolean dispose) {
        this.dispose = dispose;
        return (C) this;
    }

    public C setLimit(int limit) {
        this.limit = limit;
        return (C) this;
    }

    public void onCollect(Consumer<T> consumer) {
        this.collect = consumer;
    }

    public void onEnd(Consumer<List<T>> consumer) {
        this.end = consumer;
    }

    public void start() {
        if (!closed)
            return;

        closed = false;
        collected = new ArrayList<>();
        if (time != -1) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stop();
                }
            }, time);
        }
        jda.addEventListener(listener);
    }

    public void stop() {
        if (closed)
            return;

        closed = true;
        jda.removeEventListener(listener);
        if (end != null) {
            end.accept(collected);
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (dispose) {
            collected.clear();
        }
    }
}
