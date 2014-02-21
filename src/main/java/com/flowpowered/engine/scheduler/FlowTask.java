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

import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.flowpowered.commons.Named;
import com.flowpowered.api.scheduler.Scheduler;
import com.flowpowered.api.scheduler.Task;
import com.flowpowered.api.scheduler.TaskManager;
import com.flowpowered.api.scheduler.TaskPriority;
import com.flowpowered.api.util.concurrent.LongPrioritized;

import org.apache.commons.lang3.Validate;

/**
 * Represents a task which is executed periodically.
 */
public class FlowTask extends FutureTask<Void> implements Task, LongPrioritized {
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
     * The owner of this task
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
     * Return the last state returned by {@link #shouldExecute()}
     */
    private volatile TaskExecutionState lastExecutionState = TaskExecutionState.WAIT;

    /**
     * Creates a new task with the specified period between consecutive calls to {@link #pulse()}.
     */
    public FlowTask(TaskManager manager, Scheduler scheduler, Object owner, Runnable task, boolean sync, long delay, long period, TaskPriority priority) {
        super(task, null);
        Validate.isTrue(!sync || priority != null, "Priority cannot be null if sync!");
        this.taskId = nextTaskId.getAndIncrement();
        this.nextCallTime = new AtomicLong(manager.getUpTime() + delay);
        this.executing = new AtomicBoolean(false);
        this.owner = owner;
        this.delay = delay;
        this.period = period;
        this.sync = sync;
        this.priority = priority;
        this.manager = manager;
        this.scheduler = scheduler;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLongLived() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel() {
        this.cancel(false);
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
     * Called every 'pulse'. This method
     * updates the counters and returns whether execute() should be called
     * @return Execution state for this task
     */
    TaskExecutionState shouldExecute() {
        final TaskExecutionState execState = shouldExecuteUpdate();
        lastExecutionState = execState;
        return execState;
    }

    private TaskExecutionState shouldExecuteUpdate() {
        // Stop running if cancelled, exception, or not repeating
        if (isDone()) {
            return TaskExecutionState.STOP;
        }
        if (manager.getUpTime() >= nextCallTime.get()) {
            return TaskExecutionState.RUN;
        }
        return TaskExecutionState.WAIT;
    }

    /**
     * Return the last execution state returned by {@link #shouldExecute()}
     * @return the last state (most likely the state the task is currently in)
     */
    TaskExecutionState getLastExecutionState() {
        return lastExecutionState;
    }

    @Override
    public void run() {
        if (scheduler.isServerOverloaded()) {
            if (attemptDefer()) {
                updateCallTime(FlowScheduler.PULSE_EVERY);
                return;
            }
        }

        try {
            if (period == -1) {
                super.run();
            } else {
                super.runAndReset();
            }

            updateCallTime();
        } finally {
            executing.set(false);
        }
    }

    @Override
    public String toString() {
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
        if (!sync || priority == null) {
            return false;
        }
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
        boolean success = !isDone();
        if (!success) {
            return false;
        }
        long now = manager.getUpTime();
        if (nextCallTime.addAndGet(offset) <= now) {
            nextCallTime.set(now + 1);
        }
        return true;
    }

    @Override
    public long getPriority() {
        return nextCallTime.get();
    }
}
