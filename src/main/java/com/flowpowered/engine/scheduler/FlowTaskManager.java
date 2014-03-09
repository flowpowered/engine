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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.flowpowered.api.scheduler.Task;
import com.flowpowered.api.scheduler.TaskManager;
import com.flowpowered.api.scheduler.TaskPriority;
import com.flowpowered.engine.util.thread.AsyncManager;

public class FlowTaskManager implements TaskManager {
    private final FlowScheduler scheduler;

    /**
     * Executor to handle execution of async tasks
     */
    private final ExecutorService asyncTaskExecutor = Executors.newCachedThreadPool(new MarkedNamedThreadFactory("Async task exectuor - ", false));

    /**
     * Tasks
     */
    private final PriorityQueue<FlowTask> prioritizedTaskQueue = new PriorityQueue<>(10, new TaskComparator());
    private final LinkedTransferQueue<FlowTask> newTasks = new LinkedTransferQueue<>();
    private final ConcurrentHashMap<Integer, FlowTask> activeTasks = new ConcurrentHashMap<>();

    /**
     * The primary worlds thread in which pulse() is called.
     */
    private Thread primaryThread;
    private final AtomicLong upTime;
 
    public FlowTaskManager(FlowScheduler scheduler) {
        this(scheduler, null, 0L);
    }

    /**
     * Creates a new task scheduler.
     */
    public FlowTaskManager(FlowScheduler scheduler, AsyncManager taskManager, long age) {
        this.scheduler = scheduler;
        primaryThread = Thread.currentThread();
        this.upTime = new AtomicLong(age);
        
    }

    /**
     * Stops the scheduler and all tasks.
     */
    public void stop() {
        cancelAllTasks();
        asyncTaskExecutor.shutdown();
    }

    /**
     * Schedules the specified task.
     *
     * @param task The task.
     * @throws IllegalArgumentException if the task is already scheduled
     */
    FlowTask schedule(FlowTask task) throws IllegalArgumentException {
        if (!newTasks.add(task)) {
            throw new IllegalArgumentException("Task is already scheduled!");
        }
        activeTasks.put(task.getTaskId(), task);
        return task;
    }

    /**
     * Returns true if the current {@link Thread} is the server's primary thread.
     */
    public boolean isPrimaryThread() {
        return Thread.currentThread() == primaryThread;
    }

    /**
     * Adds new tasks and updates existing tasks, removing them if necessary.
     *
     * TODO: Add watchdog system to make sure ticks advance
     */
    public void heartbeat(long delta) {
        primaryThread = Thread.currentThread();
        long upTime = this.upTime.addAndGet(delta);

        newTasks.drainTo(prioritizedTaskQueue);

        FlowTask task;
        while ((task = prioritizedTaskQueue.peek()) != null) {
            if (task.getNextCallTime() > upTime) {
                break;
            }
            prioritizedTaskQueue.remove();
            activeTasks.remove(task.getTaskId());
            if (task.isSync()) {
                task.run();
            } else {
                asyncTaskExecutor.submit(task);
            }
        }
    }

    public <T> Future<T> callSyncMethod(Object owner, Callable<T> task) {
        return callSyncMethod(owner, task, TaskPriority.NORMAL);
    }

    @Override
    public <T> Future<T> callSyncMethod(Object owner, Callable<T> task, TaskPriority priority) {
        FutureTask<T> future = new FutureTask<T>(task);
        runTask(owner, future, priority);
        return future;
    }

    public <T> T syncIfNeeded(Callable<T> task) throws Exception {
        if (isPrimaryThread()) {
            return task.call();
        } else {
            return callSyncMethod(null, task).get();
        }
    }

    @Override
    public FlowTask runTask(Object owner, Runnable task) throws IllegalArgumentException {
        return runTaskLater(owner, task, 0, TaskPriority.NORMAL);
    }

    @Override
    public FlowTask runTask(Object owner, Runnable task, TaskPriority priority) {
        return runTaskLater(owner, task, 0, priority);
    }

    @Override
    public FlowTask runTaskAsynchronously(Object owner, Runnable task) throws IllegalArgumentException {
        return runTaskLaterAsynchronously(owner, task, 0);
    }

    @Override
    public FlowTask runTaskLater(Object owner, Runnable task, long delay, TaskPriority priority) throws IllegalArgumentException {
        return runTaskTimer(owner, task, delay, -1, priority);
    }

    @Override
    public FlowTask runTaskLaterAsynchronously(Object owner, Runnable task, long delay) throws IllegalArgumentException {
        return runTaskTimerAsynchronously(owner, task, delay, -1);
    }

    @Override
    public FlowTask runTaskTimer(Object owner, Runnable task, long delay, long period, TaskPriority priority) throws IllegalArgumentException {
        return schedule(new FlowTask(this, scheduler, owner, task, true, delay, period, priority));
    }

    @Override
    public FlowTask runTaskTimerAsynchronously(Object owner, Runnable task, long delay, long period) throws IllegalArgumentException {
        return schedule(new FlowTask(this, scheduler, owner, task, false, delay, period, null));
    }

    @Override
    public void cancelTask(Task task) {
        cancelTask(task.getTaskId());
    }

    @Override
    public void cancelTask(int taskId) {
        FlowTask task = activeTasks.remove(taskId);
        newTasks.remove(task);
        prioritizedTaskQueue.remove(task);
    }

    @Override
    public void cancelTasks(Object plugin) {
        ArrayList<FlowTask> tasks = new ArrayList<>(activeTasks.values());
        for (FlowTask task : tasks) {
            if (task.getOwner() == plugin) {
                cancelTask(task);
            }
        }
    }

    @Override
    public void cancelAllTasks() {
        ArrayList<FlowTask> tasks = new ArrayList<>(activeTasks.values());
        for (FlowTask task : tasks) {
            cancelTask(task);
        }
    }

    @Override
    public boolean isQueued(int taskId) {
        return activeTasks.containsKey(taskId);
    }

    /**
     * Returns tasks that still have at least one run remaining
     * @return the tasks to be run
     */
    @Override
    public List<Task> getPendingTasks() {
        return new ArrayList<Task>(activeTasks.values());
    }

    @Override
    public long getUpTime() {
        return upTime.get();
    }
 
    public boolean waitForAsyncTasks(long timeout) {
        try {
            asyncTaskExecutor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            return false;
        }
        return true;
    }

    public void runCoreAsyncTask(Runnable runnable) {
        asyncTaskExecutor.execute(runnable);
    }

    private static class TaskComparator implements Comparator<FlowTask> {
        @Override
        public int compare(FlowTask o1, FlowTask o2) {
            return o1.getNextCallTime() < o2.getNextCallTime() ? 1 : (o2.getNextCallTime() < o1.getNextCallTime() ? -1 : 0);
        }
    }
}
