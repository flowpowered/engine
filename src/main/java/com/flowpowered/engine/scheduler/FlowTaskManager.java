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
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.flowpowered.api.Flow;
import com.flowpowered.api.scheduler.Scheduler;
import com.flowpowered.api.scheduler.Task;
import com.flowpowered.api.scheduler.TaskManager;
import com.flowpowered.api.scheduler.TaskPriority;
import com.flowpowered.api.scheduler.Worker;
import com.flowpowered.engine.util.thread.AsyncManager;

public class FlowTaskManager implements TaskManager {
    private final ConcurrentHashMap<FlowTask, FlowWorker> activeWorkers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, FlowTask> activeTasks = new ConcurrentHashMap<>();
    private final TaskPriorityQueue taskQueue;
    private final AtomicBoolean alive;
    private final AtomicLong upTime;
    private final Object scheduleLock = new Object();
    private final FlowScheduler scheduler;
    private final ExecutorService pool = Executors.newFixedThreadPool(20, new MarkedNamedThreadFactory("Scheduler Thread Pool Thread", false));

    public FlowTaskManager(FlowScheduler scheduler) {
        this(scheduler, null, 0L);
    }

    public FlowTaskManager(FlowScheduler scheduler, AsyncManager manager) {
        this(scheduler, manager, 0L);
    }

    public FlowTaskManager(FlowScheduler scheduler, AsyncManager manager, long age) {
        this.taskQueue = new TaskPriorityQueue(manager, FlowScheduler.PULSE_EVERY / 4);
        this.alive = new AtomicBoolean(true);
        this.upTime = new AtomicLong(age);
        this.scheduler = scheduler;
    }

    @Override
    public Task scheduleSyncDelayedTask(Object plugin, Runnable task) {
        return scheduleSyncDelayedTask(plugin, task, 0, TaskPriority.CRITICAL);
    }

    @Override
    public Task scheduleSyncDelayedTask(Object plugin, Runnable task, TaskPriority priority) {
        return scheduleSyncDelayedTask(plugin, task, 0, priority);
    }

    @Override
    public Task scheduleSyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority) {
        return scheduleSyncRepeatingTask(plugin, task, delay, -1, priority);
    }

    @Override
    public Task scheduleSyncRepeatingTask(Object plugin, Runnable task, long delay, long period, TaskPriority priority) {
        return schedule(new FlowTask(this, scheduler, plugin, task, true, delay, period, priority, false));
    }

    @Override
    public Task scheduleAsyncTask(Object plugin, Runnable task) {
        return scheduleAsyncTask(plugin, task, false);
    }

    @Override
    public Task scheduleAsyncTask(Object plugin, Runnable task, boolean longLife) {
        return scheduleAsyncDelayedTask(plugin, task, 0, TaskPriority.CRITICAL, longLife);
    }

    @Override
    public Task scheduleAsyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority) {
        return scheduleAsyncDelayedTask(plugin, task, delay, priority, true);
    }

    @Override
    public Task scheduleAsyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority, boolean longLife) {
        if (!alive.get()) {
            return null;
        } else {
            return schedule(new FlowTask(this, scheduler, plugin, task, false, delay, -1, priority, longLife));
        }
    }

    @Override
    public <T> Future<T> callSyncMethod(Object plugin, Callable<T> task, TaskPriority priority) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void heartbeat(long delta) {
        long upTime = this.upTime.addAndGet(delta);

        Queue<FlowTask> q;

        while ((q = taskQueue.poll(upTime)) != null) {
            boolean checkRequired = !taskQueue.isFullyBelowThreshold(q, upTime);
            Iterator<FlowTask> itr = q.iterator();
            while (itr.hasNext()) {
                FlowTask currentTask = itr.next();
                if (checkRequired && currentTask.getPriority() > upTime) {
                    continue;
                }

                itr.remove();
                currentTask.setUnqueued();

                if (!currentTask.isAlive()) {
                    continue;
                } else if (currentTask.isSync()) {
                    currentTask.pulse();
                    repeatSchedule(currentTask);
                } else {
                    scheduler.getEngine().getLogger().info("Async repeating task submitted");
                }
            }
            if (taskQueue.complete(q, upTime)) {
                break;
            }
        }
    }

    public void cancelTask(FlowTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null!");
        }
        synchronized (scheduleLock) {
            task.stop();
            if (taskQueue.remove(task)) {
                removeTask(task);
            }
        }
        if (!task.isSync()) {
            FlowWorker worker = activeWorkers.get(task);
            if (worker != null) {
                worker.interrupt();
            }
        }
    }

    public Task schedule(FlowTask task) {
        synchronized (scheduleLock) {
            if (!addTask(task)) {
                return task;
            }
            if (!task.isSync()) {
                FlowWorker worker = new FlowWorker(task, this);
                addWorker(worker, task);
                worker.start(pool);
            } else {
                taskQueue.add(task);
            }
            return task;
        }
    }

    protected Task repeatSchedule(FlowTask task) {
        synchronized (scheduleLock) {
            if (task.isAlive()) {
                schedule(task);
            } else {
                removeTask(task);
            }
        }
        return task;
    }

    public void addWorker(FlowWorker worker, FlowTask task) {
        activeWorkers.put(task, worker);
    }

    public boolean removeWorker(FlowWorker worker, FlowTask task) {
        return activeWorkers.remove(task, worker);
    }

    public boolean addTask(FlowTask task) {
        activeTasks.put(task.getTaskId(), task);
        if (!alive.get()) {
            cancelTask(task);
            return false;
        }
        return true;
    }

    public boolean removeTask(FlowTask task) {
        return activeTasks.remove(task.getTaskId(), task);
    }

    @Override
    public boolean isQueued(int taskId) {
        return activeTasks.containsKey(taskId);
    }

    @Override
    public void cancelTask(int taskId) {
        cancelTask(activeTasks.get(taskId));
    }

    @Override
    public void cancelTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null!");
        }
        cancelTask(activeTasks.get(task.getTaskId()));
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
    public List<Worker> getActiveWorkers() {
        return new ArrayList<Worker>(activeWorkers.values());
    }

    public boolean waitForAsyncTasks(long timeout) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + timeout) {
            try {
                if (activeWorkers.isEmpty()) {
                    return true;
                }
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                return false;
            }
        }
        return false;
    }

    @Override
    public List<Task> getPendingTasks() {
        List<FlowTask> tasks = taskQueue.getTasks();
        List<Task> list = new ArrayList<>(tasks.size());
        for (FlowTask t : tasks) {
            list.add(t);
        }
        return list;
    }

    public boolean shutdown() {
        return shutdown(1);
    }

    public boolean shutdown(long timeout) {
        alive.set(false);
        pool.shutdown();
        cancelAllTasks();
        return true;
    }

    @Override
    public long getUpTime() {
        return upTime.get();
    }
}
