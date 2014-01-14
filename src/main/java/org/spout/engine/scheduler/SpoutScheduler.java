/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.engine.scheduler;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.spout.api.Client;
import org.spout.api.Engine;
import org.spout.api.scheduler.Scheduler;
import org.spout.api.scheduler.Task;
import org.spout.api.scheduler.TaskPriority;
import org.spout.api.scheduler.Worker;
import org.spout.engine.scheduler.input.InputThread;
import org.spout.engine.scheduler.render.RenderThread;
import org.spout.engine.util.thread.AsyncManager;

/**
 * A class which handles scheduling for the engine {@link SpoutTask}s.<br> <br> Tasks can be submitted to the scheduler for execution by the main thread. These tasks are executed during a period where
 * none of the auxiliary threads are executing.<br> <br> Each tick consists of a number of stages. Each stage is executed in parallel, but the next stage is not started until all threads have
 * completed the previous stage.<br> <br> Except for executing queued serial tasks, all threads are run in parallel. The full sequence is as follows:<br> <ul> <li>Single Thread <ul> <li><b>Execute
 * queued tasks</b><br> Tasks that are submitted for execution are executed one at a time. </ul> <li>Parallel Threads <ul> <li><b>Stage 1</b><br> This is the first stage of execution. Most Events are
 * generated during this stage and the API is fully open for use. - chunks are populated. <li><b>Stage 2</b><br> During this stage, entity collisions are handled. <li><b>Finalize Tick</b><br> During
 * this stage - entities are moved between entity managers. - chunks are compressed if necessary. <li><b>Pre-snapshot</b><br> This is a MONITOR stage, data is stable and no modifications are allowed.
 * <li><b>Copy Snapshot</b><br> During this stage all live values are copied to their stable snapshot. Data is unstable so no reads are permitted during this stage. </ul> </ul>
 */
public final class SpoutScheduler implements Scheduler {
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
    private final SpoutTaskManager taskManager;
    // SchedulerElements
    private final MainThread mainThread;
    private final RenderThread renderThread;
    private final InputThread inputThread;

	/**
	 * Creates a new task scheduler.
	 */
	public SpoutScheduler(Engine engine) {
		mainThread = new MainThread(this);

		if (engine.getPlatform().isClient()) {
            inputThread = new InputThread(this);
			renderThread = new RenderThread((Client) engine, this);
		} else {
            inputThread = null;
			renderThread = null;
		}
		taskManager = new SpoutTaskManager(this);
	}

	public void startMainThread() {
		if (mainThread.isRunning()) {
			throw new IllegalStateException("Attempt was made to start the main thread twice");
		}

		mainThread.start();
	}

	public void startClientThreads() {
		if (renderThread.isRunning() || inputThread.isRunning()) {
			throw new IllegalStateException("Attempt was made to start the client threads twice");
		}
        renderThread.start();
        inputThread.start();
	}

	/**
	 * Stops the scheduler
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
	public Task scheduleSyncDelayedTask(Object plugin, Runnable task) {
		return taskManager.scheduleSyncDelayedTask(plugin, task);
	}

	@Override
	public Task scheduleSyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority) {
		return taskManager.scheduleSyncDelayedTask(plugin, task, delay, priority);
	}

	@Override
	public Task scheduleSyncDelayedTask(Object plugin, Runnable task, TaskPriority priority) {
		return taskManager.scheduleSyncDelayedTask(plugin, task, priority);
	}

	@Override
	public Task scheduleSyncRepeatingTask(Object plugin, Runnable task, long delay, long period, TaskPriority priority) {
		return taskManager.scheduleSyncRepeatingTask(plugin, task, delay, period, priority);
	}

	@Override
	public Task scheduleAsyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority) {
		return taskManager.scheduleAsyncDelayedTask(plugin, task, delay, priority);
	}

	@Override
	public Task scheduleAsyncDelayedTask(Object plugin, Runnable task, long delay, TaskPriority priority, boolean longLife) {
		return taskManager.scheduleAsyncDelayedTask(plugin, task, delay, priority, longLife);
	}

	@Override
	public Task scheduleAsyncTask(Object plugin, Runnable task) {
		return taskManager.scheduleAsyncTask(plugin, task);
	}

	@Override
	public Task scheduleAsyncTask(Object plugin, Runnable task, boolean longLife) {
		return taskManager.scheduleAsyncTask(plugin, task, longLife);
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
	public List<Worker> getActiveWorkers() {
		return taskManager.getActiveWorkers();
	}

	@Override
	public List<Task> getPendingTasks() {
		return taskManager.getPendingTasks();
	}

	@Override
	public long getUpTime() {
		return taskManager.getUpTime();
	}

    public SpoutTaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public boolean isServerOverloaded() {
        return false;
    }

    public MainThread getMainThread() {
        return mainThread;
    }

    public RenderThread getRenderThread() {
        return renderThread;
    }

    public InputThread getInputThread() {
        return inputThread;
    }

	/**
	 * Adds an async manager to the scheduler
	 */
	public void addAsyncManager(AsyncManager manager) {
		mainThread.addAsyncManager(manager);
	}

	/**
	 * Removes an async manager from the scheduler
	 */
	public void removeAsyncManager(AsyncManager manager) {
		mainThread.removeAsyncManager(manager);
	}

    public void runCoreAsyncTask(Runnable r) {
        mainThread.executorService.submit(r);
    }
}
