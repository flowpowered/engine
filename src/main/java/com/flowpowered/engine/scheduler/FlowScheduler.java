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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.flowpowered.api.Engine;
import com.flowpowered.api.scheduler.Scheduler;
import com.flowpowered.api.scheduler.Task;
import com.flowpowered.api.scheduler.TaskPriority;
import com.flowpowered.engine.FlowClient;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.scheduler.input.InputThread;
import com.flowpowered.engine.scheduler.render.RenderThread;
import com.flowpowered.engine.util.thread.AsyncManager;

/**
 * A class which handles scheduling for the engine {@link FlowTask}s.<br> <br> Tasks can be submitted to the scheduler for execution by the main thread. These tasks are executed during a period where
 * none of the auxiliary threads are executing.<br> <br> Each tick consists of a number of stages. Each stage is executed in parallel, but the next stage is not started until all threads have
 * completed the previous stage.<br> <br> Except for executing queued serial tasks, all threads are run in parallel. The full sequence is as follows:<br> <ul> <li>Single Thread <ul> <li><b>Execute
 * queued tasks</b><br> Tasks that are submitted for execution are executed one at a time. </ul> <li>Parallel Threads <ul> <li><b>Stage 1</b><br> This is the first stage of execution. Most Events are
 * generated during this stage and the API is fully open for use. - chunks are populated. <li><b>Stage 2</b><br> During this stage, entity collisions are handled. <li><b>Finalize Tick</b><br> During
 * this stage - entities are moved between entity managers. - chunks are compressed if necessary. <li><b>Pre-snapshot</b><br> This is a MONITOR stage, data is stable and no modifications are allowed.
 * <li><b>Copy Snapshot</b><br> During this stage all live values are copied to their stable snapshot. Data is unstable so no reads are permitted during this stage. </ul> </ul>
 */
public final class FlowScheduler implements Scheduler {
    /**
     * The number of milliseconds between pulses.
     */
    public static final int PULSE_EVERY = 50;
    /**
     * A time that is at least 1 Pulse below the maximum time instant
     */
    public static final long END_OF_THE_WORLD = Long.MAX_VALUE - PULSE_EVERY;
    /**
     * Target Frames per Second for the renderer
     */
    public static final int TARGET_FPS = 60;
    private final FlowTaskManager taskManager;
    private final FlowEngine engine;
    // SchedulerElements
    private final MainThread mainThread;
    private RenderThread renderThread;
    private InputThread inputThread;

    /**
     * Creates a new task scheduler.
     */
    public FlowScheduler(FlowEngine engine) {
        this.engine = engine;
        mainThread = new MainThread(this);
        taskManager = new FlowTaskManager(this);
    }

    public void startMainThread() {
        if (mainThread.isRunning()) {
            throw new IllegalStateException("Attempt was made to start the main thread twice");
        }
        mainThread.start();
    }

    public void startClientThreads(FlowClient client) {
        if (renderThread != null && renderThread.isRunning() || inputThread != null && inputThread.isRunning()) {
            throw new IllegalStateException("Attempt was made to start the client threads twice");
        }

        inputThread = new InputThread(client);
        renderThread = new RenderThread(client);
        renderThread.start();
        inputThread.start();
    }

    /**
     * Stops the scheduler. Should only be called from Engine#stop().
     */
    public void stop() {
        mainThread.stop();
        if (renderThread != null) {
            renderThread.stop();
        }
        if (inputThread != null) {
            inputThread.stop();
        }
    }

    @Override
    public FlowTask runTask(Object owner, Runnable task) throws IllegalArgumentException {
        return taskManager.runTask(owner, task);
    }

    @Override
    public FlowTask runTask(Object owner, Runnable task, TaskPriority priority) {
        return taskManager.runTask(owner, task, priority);
    }

    @Override
    public FlowTask runTaskAsynchronously(Object owner, Runnable task) throws IllegalArgumentException {
        return taskManager.runTaskAsynchronously(owner, task);
    }

    @Override
    public FlowTask runTaskLater(Object owner, Runnable task, long delay, TaskPriority priority) throws IllegalArgumentException {
        return taskManager.runTaskLater(owner, task, delay, priority);
    }

    @Override
    public FlowTask runTaskLaterAsynchronously(Object owner, Runnable task, long delay) throws IllegalArgumentException {
        return taskManager.runTaskLaterAsynchronously(owner, task, delay);
    }

    @Override
    public FlowTask runTaskTimer(Object owner, Runnable task, long delay, long period, TaskPriority priority) throws IllegalArgumentException {
        return taskManager.runTaskTimer(owner, task, delay, period, priority);
    }

    @Override
    public FlowTask runTaskTimerAsynchronously(Object owner, Runnable task, long delay, long period) throws IllegalArgumentException {
        return taskManager.runTaskTimerAsynchronously(owner, task, delay, period);
    }

    @Override
    public <T> Future<T> callSyncMethod(Object plugin, Callable<T> task, TaskPriority priority) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isQueued(int taskId) {
        return taskManager.isQueued(taskId);
    }

    @Override
    public void cancelTask(int taskId) {
        taskManager.cancelTask(taskId);
    }

    @Override
    public void cancelTask(Task task) {
        taskManager.cancelTask(task);
    }

    @Override
    public void cancelTasks(Object plugin) {
        taskManager.cancelTasks(plugin);
    }

    @Override
    public void cancelAllTasks() {
        taskManager.cancelAllTasks();
    }

    @Override
    public List<Task> getPendingTasks() {
        return taskManager.getPendingTasks();
    }

    @Override
    public long getUpTime() {
        return taskManager.getUpTime();
    }

    public FlowTaskManager getTaskManager() {
        return taskManager;
    }

    public FlowEngine getEngine() {
        return engine;
    }

    @Override
    public boolean isServerOverloaded() {
        return false;
    }

    public RenderThread getRenderThread() {
        return renderThread;
    }

    public InputThread getInputThread() {
        return inputThread;
    }
}
