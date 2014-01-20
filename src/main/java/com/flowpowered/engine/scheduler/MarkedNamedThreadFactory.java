package com.flowpowered.engine.scheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MarkedNamedThreadFactory implements ThreadFactory {
    private final AtomicInteger idCounter = new AtomicInteger();
    private final String namePrefix;
    private final boolean daemon;

    public MarkedNamedThreadFactory(String namePrefix, boolean daemon) {
        this.namePrefix = namePrefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "Executor{" + namePrefix + "-" + idCounter.getAndIncrement() + "}");
        thread.setDaemon(daemon);
        return thread;
    }
}
