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
package com.flowpowered.api.scheduler;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface TaskManager {
    /**
     * Schedules a once off task to occur as soon as possible This task will be executed by the main server thread.
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @return the task
     */
    public Task scheduleSyncDelayedTask(Object plugin, Runnable task);

    /**
     * Schedules a once off task to occur as soon as possible This task will be executed by the main server thread.
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @param priority the priority of the task
     * @return the task
     */
    public Task scheduleSyncDelayedTask(Object plugin, Runnable task, TaskPriority priority);

    /**
     * Schedules a once off task to occur after a delay.   This task will be executed by the main server thread
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @param delay the delay, in ms, before the task starts
     * @param priority the priority of the task
     * @return the task
     */
    public Task scheduleSyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority);

    /**
     * Schedules a repeating task This task will be executed by the main server thread.  The repeat will not be started if the task until the previous repeat has completed running.
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @param delay the delay, in ms, before the task starts
     * @param period the repeat period, in ms, of the task, or <= 0 to indicate a single shot task
     * @param priority the priority of the task
     * @return the task
     */
    public Task scheduleSyncRepeatingTask(Object plugin, Runnable task, long delay, long period, TaskPriority priority);

    /**
     * Schedules a once off short lived task to occur as soon as possible.  This task will be executed by a thread managed by the scheduler
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @return the task id of the task
     */
    public Task scheduleAsyncTask(Object plugin, Runnable task);

    /**
     * Schedules a once off task to occur as soon as possible.  This task will be executed by a thread managed by the scheduler
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @param longLife indicates that the thread is long lived
     * @return the tas
     */
    public Task scheduleAsyncTask(Object plugin, Runnable task, boolean longLife);

    /**
     * Schedules a once off short lived task to occur after a delay.  This task will be executed by a thread managed by the scheduler.
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @param delay the delay, in ms, before the task starts
     * @param priority the priority of the task
     * @return the task
     */
    public Task scheduleAsyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority);

    /**
     * Schedules a once off task to occur after a delay.  This task will be executed by a thread managed by the scheduler.
     *
     * @param plugin the owner of the task
     * @param task the task to execute
     * @param delay the delay, in ms, before the task starts
     * @param priority the priority of the task
     * @param longLife indicates that the thread is long lived
     * @return the task
     */
    public Task scheduleAsyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority, boolean longLife);

    /**
     * Calls a method on the main thread and returns a Future object This task will be executed by the main server thread <br/>
     *
     * <b>Note:</b> The Future.get() methods must NOT be called from the main thread<br/> <b>Note 2:</b> There is at least an average of 10ms latency until the isDone() method returns true<br/>
     *
     * @param plugin the owner of the task
     * @param task the Callable to execute
     * @param priority the priority of the task
     * @return Future Future object related to the task
     */
    public <T> Future<T> callSyncMethod(Object plugin, Callable<T> task, TaskPriority priority);

    /**
     * True if the task is an actively scheduled task
     *
     * @return actived scheduled
     */
    public boolean isQueued(int taskId);

    /**
     * Removes task from scheduler
     */
    public void cancelTask(int taskId);

    /**
     * Removes task from scheduler
     */
    public void cancelTask(Task task);

    /**
     * Removes all tasks associated with a particular object from the scheduler
     */
    public void cancelTasks(Object plugin);

    /**
     * Removes all tasks from the scheduler
     */
    public void cancelAllTasks();

    /**
     * Returns a list of all active workers.
     *
     * This list contains asynch tasks that are being executed by separate threads.
     *
     * @return Active workers
     */
    public List<Worker> getActiveWorkers();

    /**
     * Returns a list of all pending tasks. The ordering of the tasks is not related to their order of execution.
     *
     * @return Active workers
     */
    public List<Task> getPendingTasks();

    /**
     * Gets the up time for the scheduler.  This is the time since server started for the main schedulers and the age of the world for the Region based schedulers.<br> <br> It is updated once per tick.
     *
     * @return the up time in milliseconds
     */
    public long getUpTime();
}
