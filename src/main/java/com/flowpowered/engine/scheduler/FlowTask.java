/*
 * This file is part of Flow Engine, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.engine.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.commons.Named;
import com.flowpowered.api.scheduler.Scheduler;
import com.flowpowered.api.scheduler.Task;
import com.flowpowered.api.scheduler.TaskManager;
import com.flowpowered.api.scheduler.TaskPriority;
import com.flowpowered.api.util.concurrent.LongPrioritized;

/**
 * Represents a task which is executed periodically.
 */
public class FlowTask implements Task, LongPrioritized {
    /**
     * The next task ID pending.
     */
    private final static AtomicInteger nextTaskId = new AtomicInteger(0);
    /**
     * The ID of this task.
     */
    private final int taskId;
    /**
     * The task priority
     */
    private final TaskPriority priority;
    /**
     * The Runnable this task is representing.
     */
    private final Runnable task;
    /**
     * The Plugin that owns this task
     */
    private final Object owner;
    /**
     * The number of ticks before the call to the Runnable.
     */
    private final long delay;
    /**
     * The number of ticks between each call to the Runnable.
     */
    private final long period;
    /**
     * Indicates if the task is a synchronous task or an async task
     */
    private final boolean sync;
    /**
     * Indicates the next scheduled time for the task to be called
     */
    private final AtomicLong nextCallTime;
    private final AtomicReference<QueueState> queueState = new AtomicReference<>(QueueState.UNQUEUED);
    /**
     * A flag indicating if the task is actually executing
     */
    private final AtomicBoolean executing;
    /**
     * Indicates if the task is being deferred and when it started
     */
    private long deferBegin = -1;
    /**
     * The manager associated with this task
     */
    private final TaskManager manager;
    /**
     * The scheduler for the engine
     */
    private final Scheduler scheduler;
    /**
     * Indicates that the task is long lived
     */
    private final boolean longLife;

    /**
     * Creates a new task with the specified period between consecutive calls to {@link #pulse()}.
     */
    public FlowTask(TaskManager manager, Scheduler scheduler, Object owner, Runnable task, boolean sync, long delay, long period, TaskPriority priority, boolean longLife) {
        this.taskId = nextTaskId.getAndIncrement();
        this.nextCallTime = new AtomicLong(manager.getUpTime() + delay);
        this.executing = new AtomicBoolean(false);
        this.owner = owner;
        this.task = task;
        this.delay = delay;
        this.period = period;
        this.sync = sync;
        this.priority = priority;
        this.manager = manager;
        this.scheduler = scheduler;
        this.longLife = longLife;
    }

    /**
     * Gets the ID of this task.
     */
    @Override
    public int getTaskId() {
        return taskId;
    }

    @Override
    public boolean isSync() {
        return sync;
    }

    @Override
    public boolean isExecuting() {
        return executing.get();
    }

    @Override
    public Object getOwner() {
        return owner;
    }

    @Override
    public boolean isAlive() {
        return !queueState.get().isDead();
    }

    @Override
    public boolean isLongLived() {
        return longLife;
    }

    @Override
    public void cancel() {
        manager.cancelTask(taskId);
    }

    public long getNextCallTime() {
        return nextCallTime.get();
    }

    protected long getPeriod() {
        return this.period;
    }

    protected long getDelay() {
        return this.delay;
    }

    /**
     * Stops this task.
     */
    public void stop() {
        remove();
    }

    /**
     * Executes the task.  The task will fail to execute if it is no longer running, if it is called early, or if it is already executing.
     *
     * @return The task successfully executed.
     */
    boolean pulse() {
        if (queueState.get().isDead()) {
            return false;
        }

        if (scheduler.isServerOverloaded()) {
            if (attemptDefer()) {
                updateCallTime(FlowScheduler.PULSE_EVERY);
                return false;
            }
        }

        if (!executing.compareAndSet(false, true)) {
            return false;
        }

        try {
            task.run();

            updateCallTime();

            if (period <= 0) {
                queueState.set(QueueState.DEAD);
            }
        } finally {
            executing.set(false);
        }

        return true;
    }

    public void remove() {
        queueState.set(QueueState.DEAD);
    }

    public boolean setQueued() {
        if (!queueState.compareAndSet(QueueState.UNQUEUED, QueueState.QUEUED)) {
            boolean success = false;
            while (!success) {
                QueueState oldState = queueState.get();
                switch (oldState) {
                    case DEAD:
                        return false;
                    case QUEUED:
                        throw new IllegalStateException("Task added in the queue twice without being removed");
                    case UNQUEUED:
                        success = queueState.compareAndSet(QueueState.UNQUEUED, QueueState.QUEUED);
                        break;
                    default:
                        throw new IllegalStateException("Unknown queue state " + oldState);
                }
            }
        }
        return true;
    }

    public boolean setUnqueued() {
        if (!queueState.compareAndSet(QueueState.QUEUED, QueueState.UNQUEUED)) {
            boolean success = false;
            while (!success) {
                QueueState oldState = queueState.get();
                switch (oldState) {
                    case DEAD:
                        return false;
                    case UNQUEUED:
                        throw new IllegalStateException("Task set as unqueued before being set as queued");
                    case QUEUED:
                        success = queueState.compareAndSet(QueueState.QUEUED, QueueState.UNQUEUED);
                        break;
                    default:
                        throw new IllegalStateException("Unknown queue state " + oldState);
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        Object owner = getOwner();
        String ownerName = owner == null || !(owner instanceof Named) ? "null" : ((Named) owner).getName();
        return this.getClass().getSimpleName() + "{" + getTaskId() + ", " + ownerName + "}";
    }

    @Override
    public int hashCode() {
        return taskId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof FlowTask) {
            FlowTask other = (FlowTask) o;
            return other.taskId == taskId;
        } else {
            return false;
        }
    }

    private boolean attemptDefer() {
        if (priority.getMaxDeferred() <= 0) {
            return false;
        }
        if (deferBegin < 0) {
            deferBegin = manager.getUpTime();
            return true;
        }

        if (manager.getUpTime() - deferBegin > priority.getMaxDeferred()) {
            deferBegin = -1;
            return false;
        }

        return true;
    }

    private void updateCallTime() {
        updateCallTime(period);
    }

    private boolean updateCallTime(long offset) {
        boolean success = setQueued();
        if (!success) {
            return false;
        }
        try {
            long now = manager.getUpTime();
            if (nextCallTime.addAndGet(offset) <= now) {
                nextCallTime.set(now + 1);
            }
        } finally {
            setUnqueued();
        }
        return true;
    }

    @Override
    public long getPriority() {
        return nextCallTime.get();
    }

    @SuppressWarnings ("unused")
    private static enum QueueState {
        QUEUED,
        UNQUEUED,
        DEAD;

        public boolean isDead() {
            return this == DEAD;
        }

        public boolean isQueued() {
            return this == QUEUED;
        }

        public boolean isUnQueued() {
            return this == UNQUEUED;
        }
    }
}
