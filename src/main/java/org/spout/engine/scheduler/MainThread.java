package org.spout.engine.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.commons.Named;

import org.spout.api.Spout;
import org.spout.api.scheduler.TickStage;
import org.spout.api.scheduler.Worker;
import org.spout.api.util.concurrent.ConcurrentList;
import org.spout.engine.util.thread.AsyncManager;
import org.spout.engine.util.thread.coretasks.CopySnapshotTask;
import org.spout.engine.util.thread.coretasks.DynamicUpdatesTask;
import org.spout.engine.util.thread.coretasks.FinalizeTask;
import org.spout.engine.util.thread.coretasks.LightingTask;
import org.spout.engine.util.thread.coretasks.ManagerRunnableFactory;
import org.spout.engine.util.thread.coretasks.PhysicsTask;
import org.spout.engine.util.thread.coretasks.PreSnapshotTask;
import org.spout.engine.util.thread.coretasks.StartTickTask;

public class MainThread extends SchedulerElement {
    private final SpoutScheduler scheduler;
	// Scheduler tasks
	private final StartTickTask[] startTickTask = new StartTickTask[] {new StartTickTask(0), new StartTickTask(1), new StartTickTask(2)};
	private final DynamicUpdatesTask dynamicUpdatesTask = new DynamicUpdatesTask();
	private final PhysicsTask physicsTask = new PhysicsTask();
	private final LightingTask lightingTask = new LightingTask();
	private final FinalizeTask finalizeTask = new FinalizeTask();
	private final PreSnapshotTask preSnapshotTask = new PreSnapshotTask();
	private final CopySnapshotTask copySnapshotTask = new CopySnapshotTask();

	/**
	 * Update count for physics and dynamic updates
	 */
	private final AtomicInteger updates = new AtomicInteger(0);
	/**
	 * The threshold before physics and dynamic updates are aborted
	 */
	private final static int UPDATE_THRESHOLD = 100000;
	/**
	 * A list of all AsyncManagers
	 */
	private final List<AsyncManager> asyncManagers = new ConcurrentList<>();
	// scheduler executor service
	private final ExecutorService executorService;

    public MainThread(SpoutScheduler scheduler) {
        super("MainThread", 20);
        this.scheduler = scheduler;
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1, new MarkedNamedThreadFactory("SpoutScheduler - async manager executor service", true));
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
            doCopySnapshot(asyncManagers);

			scheduler.getTaskManager().heartbeat(SpoutScheduler.PULSE_EVERY << 2);
			scheduler.getTaskManager().shutdown(1L);

			long delay = 2000;
			while (!scheduler.getTaskManager().waitForAsyncTasks(delay)) {
				List<Worker> workers = scheduler.getTaskManager().getActiveWorkers();
				if (workers.isEmpty()) {
					break;
				}
				Spout.info("Unable to shutdown due to async tasks still running");
				for (Worker w : workers) {
					Object owner = w.getOwner() instanceof Named ? ((Named) w.getOwner()).getName() : w.getOwner();
                    Spout.info("Task with id of " + w.getTaskId() + " owned by " + owner + " is still running");
				}
				if (delay < 8000) {
					delay <<= 1;
				}
			}
    }

    // TODO: config
	private final boolean DYNAMIC_UPDATES = true;
	private final boolean BLOCK_PHYSICS = true;
	private final boolean LIGHTING = true;

    @Override
    public void onTick(long delta) {
        // Delta is in nanos, we want millis with rounding
        delta = Math.round(delta * 1e-6d);

		TickStage.setStage(TickStage.TICKSTART);

		scheduler.getTaskManager().heartbeat(delta);

		List<AsyncManager> managers = new ArrayList<>(asyncManagers);

		TickStage.setStage(TickStage.STAGE1);

		for (int stage = 0; stage < this.startTickTask.length; stage++) {
			if (stage == 0) {
				TickStage.setStage(TickStage.STAGE1);
			} else {
				TickStage.setStage(TickStage.STAGE2P);
			}

			startTickTask[stage].setDelta(delta);

			int tickStage = stage == 0 ? TickStage.STAGE1 : TickStage.STAGE2P;

			runTasks(managers, startTickTask[stage], "Stage " + stage, tickStage);
		}

        int totalUpdates = -1;
        int lightUpdates = 0;
        int dynamicUpdates = 0;
        int physicsUpdates = 0;
        updates.set(0);
        int uD = 1;
        int uP = 1;
        while ((uD + uP) > 0 && totalUpdates < UPDATE_THRESHOLD) {
            if (DYNAMIC_UPDATES) {
                doDynamicUpdates(managers);
            }

            uD = updates.getAndSet(0);
            totalUpdates += uD;
            dynamicUpdates += uD;

            if (BLOCK_PHYSICS) {
                doPhysics(managers);
            }

            uP = updates.getAndSet(0);
            totalUpdates += uP;
            physicsUpdates += uP;
        }

        if (LIGHTING || !Spout.debugMode()) {
            doLighting(managers);
        }

        if (totalUpdates >= UPDATE_THRESHOLD) {
            Spout.warn("Block updates per tick of " + totalUpdates + " exceeded the threshold " + UPDATE_THRESHOLD + "; " + dynamicUpdates + " dynamic updates, " + physicsUpdates + " block physics updates and " + lightUpdates + " lighting updates");
        }

        doFinalizeTick(managers);

        doCopySnapshot(managers);
    }


	private void doPhysics(List<AsyncManager> managers) {
		int passStartUpdates = updates.get() - 1;
		int startUpdates = updates.get();
		while (passStartUpdates < updates.get() && updates.get() < startUpdates + UPDATE_THRESHOLD) {
			passStartUpdates = updates.get();
			this.runTasks(managers, physicsTask, "Physics", TickStage.GLOBAL_PHYSICS, TickStage.PHYSICS);
		}
	}

	private void doDynamicUpdates(List<AsyncManager> managers) {
		int passStartUpdates = updates.get() - 1;
		int startUpdates = updates.get();

		TickStage.setStage(TickStage.GLOBAL_DYNAMIC_BLOCKS);

		long earliestTime = SpoutScheduler.END_OF_THE_WORLD;

		for (AsyncManager e : managers) {
			long firstTime = e.getFirstDynamicUpdateTime();
			if (firstTime < earliestTime) {
				earliestTime = firstTime;
			}
		}

		while (passStartUpdates < updates.get() && updates.get() < startUpdates + UPDATE_THRESHOLD) {
			passStartUpdates = updates.get();

			long threshold = earliestTime + SpoutScheduler.PULSE_EVERY - 1;

			dynamicUpdatesTask.setThreshold(threshold);

			this.runTasks(managers, dynamicUpdatesTask, "Dynamic Blocks", TickStage.GLOBAL_DYNAMIC_BLOCKS, TickStage.DYNAMIC_BLOCKS);
		}
	}

	private void doLighting(List<AsyncManager> managers) {
		this.runTasks(managers, lightingTask, "Lighting", TickStage.LIGHTING);
	}


	private void doFinalizeTick(List<AsyncManager> managers) {
		this.runTasks(managers, finalizeTask, "Finalize", TickStage.FINALIZE);
	}

	private void doCopySnapshot(List<AsyncManager> managers) {
		this.runTasks(managers, preSnapshotTask, "Pre-snapshot", TickStage.PRESNAPSHOT);

		this.runTasks(managers, copySnapshotTask, "Copy-snapshot", TickStage.SNAPSHOT);
	}

	private void runTasks(List<AsyncManager> managers, ManagerRunnableFactory taskFactory, String stageString, int tickStage) {
		runTasks(managers, taskFactory, stageString, tickStage, tickStage);
	}

	private void runTasks(List<AsyncManager> managers, ManagerRunnableFactory taskFactory, String stageString, int globalStage, int localStage) {
		int maxSequence = taskFactory.getMaxSequence();
		for (int s = taskFactory.getMinSequence(); s <= maxSequence; s++) {
			if (s == -1) {
				TickStage.setStage(localStage);
			} else {
				TickStage.setStage(globalStage);
			}
			List<Future<?>> futures = new ArrayList<>(managers.size());
			for (AsyncManager manager : managers) {
				if (s == -1 || s == manager.getSequence()) {
					Runnable r = taskFactory.getTask(manager, s);
					if (r != null) {
						futures.add(executorService.submit(r));
					}
				}
			}
			forLoop:
			for (int i = 0; i < futures.size(); i++) {
				boolean done = false;
				while (!done) {
					try {
						Future<?> f = futures.get(i);
						if (!f.isDone()) {
							f.get(SpoutScheduler.PULSE_EVERY, TimeUnit.MILLISECONDS);
						}
						done = true;
					} catch (InterruptedException e) {
						Spout.info("Warning: main thread interrupted while waiting on tick stage task, " + taskFactory.getClass().getName());
						break forLoop;
					} catch (ExecutionException e) {
						Spout.info("Exception thrown when executing task, " + taskFactory.getClass().getName() + ", " + e.getMessage());
						e.printStackTrace();
						Spout.info("Caused by");
						e.getCause().printStackTrace();
						done = true;
					} catch (TimeoutException e) {
					}
				}
			}
		}
	}

	/**
	 * Adds an async manager to the scheduler
	 */
	public boolean addAsyncManager(AsyncManager manager) {
		return asyncManagers.add(manager);
	}

	/**
	 * Removes an async manager from the scheduler
	 */
	public boolean removeAsyncManager(AsyncManager manager) {
		return asyncManagers.remove(manager);
	}

}
