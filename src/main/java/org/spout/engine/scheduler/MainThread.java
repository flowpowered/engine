package org.spout.engine.scheduler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.flowpowered.commons.Named;
import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.commons.ticking.TickingElement;

import org.spout.api.Spout;
import org.spout.api.scheduler.TickStage;
import org.spout.api.scheduler.Worker;
import org.spout.engine.geo.region.RegionGenerator;
import org.spout.engine.util.thread.AsyncManager;
import org.spout.engine.util.thread.LoggingThreadPoolExecutor;
import org.spout.engine.util.thread.coretasks.CopySnapshotTask;
import org.spout.engine.util.thread.coretasks.LocalDynamicUpdatesTask;
import org.spout.engine.util.thread.coretasks.GlobalDynamicUpdatesTask;
import org.spout.engine.util.thread.coretasks.FinalizeTask;
import org.spout.engine.util.thread.coretasks.LightingTask;
import org.spout.engine.util.thread.coretasks.ManagerRunnable;
import org.spout.engine.util.thread.coretasks.ManagerRunnableFactory;
import org.spout.engine.util.thread.coretasks.LocalPhysicsTask;
import org.spout.engine.util.thread.coretasks.GlobalPhysicsTask;
import org.spout.engine.util.thread.coretasks.PreSnapshotTask;
import org.spout.engine.util.thread.coretasks.StartTickTask;

public class MainThread extends TickingElement {
    private final SpoutScheduler scheduler;
    private final AtomicLong currentDelta = new AtomicLong(0);
    private final ManagerRunnableFactory[] managerRunnableFactories = new ManagerRunnableFactory[] {
        null,//TickStart has no ManagerRunnableFactory
        new StartTickTask(0, currentDelta),
        new StartTickTask(1, currentDelta),
        new LocalDynamicUpdatesTask(),
        new GlobalDynamicUpdatesTask(),
        new LocalPhysicsTask(),
        new GlobalPhysicsTask(),
        new LightingTask(),
        new FinalizeTask(),
        new PreSnapshotTask(),
        new CopySnapshotTask()
    };

    // TODO: make sure that this is safe throughout the whole tick
    // SnapshotableReference?
    @SuppressWarnings("unchecked")
    private final Set<ManagerRunnable>[][] managerRunnables = new Set[managerRunnableFactories.length][];

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
	//private final List<AsyncManager> asyncManagers = new ConcurrentList<>();
	// scheduler executor service
	protected final ExecutorService executorService;
    private final TPSMonitor tpsMonitor = new TPSMonitor();

    public MainThread(SpoutScheduler scheduler) {
        super("MainThread", 20);
        this.scheduler = scheduler;
        final int nThreads = Runtime.getRuntime().availableProcessors() * 2 + 1;
		executorService = LoggingThreadPoolExecutor.newFixedThreadExecutorWithMarkedName(nThreads, "SpoutScheduler - AsyncManager executor service");
    }

    @Override
    public void onStart() {
        tpsMonitor.start();
    }

    @Override
    public void onStop() {
            doCopySnapshot();
            RegionGenerator.shutdownExecutorService();
            RegionGenerator.awaitExecutorServiceTermination();

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
        this.currentDelta.set(delta);

		TickStage.setStage(TickStage.TICKSTART);

		scheduler.getTaskManager().heartbeat(delta);

        runTasks(TickStage.STAGE1);

        runTasks(TickStage.STAGE2P);

        int totalUpdates = -1;
        int lightUpdates = 0;
        int dynamicUpdates = 0;
        int physicsUpdates = 0;
        updates.set(0);
        int uD = 1;
        int uP = 1;
        while ((uD + uP) > 0 && totalUpdates < UPDATE_THRESHOLD) {
            if (DYNAMIC_UPDATES) {
                doDynamicUpdates();
            }

            uD = updates.getAndSet(0);
            totalUpdates += uD;
            dynamicUpdates += uD;

            if (BLOCK_PHYSICS) {
                doPhysics();
            }

            uP = updates.getAndSet(0);
            totalUpdates += uP;
            physicsUpdates += uP;
        }

        if (LIGHTING) {
            doLighting();
        }

        if (totalUpdates >= UPDATE_THRESHOLD) {
            Spout.warn("Block updates per tick of " + totalUpdates + " exceeded the threshold " + UPDATE_THRESHOLD + "; " + dynamicUpdates + " dynamic updates, " + physicsUpdates + " block physics updates and " + lightUpdates + " lighting updates");
        }

        doFinalizeTick();

        doCopySnapshot();
        tpsMonitor.update();
    }


	private void doPhysics() {
		int passStartUpdates = updates.get() - 1;
		int startUpdates = updates.get();
		while (passStartUpdates < updates.get() && updates.get() < startUpdates + UPDATE_THRESHOLD) {
			passStartUpdates = updates.get();
            runTasks(TickStage.PHYSICS);
            runTasks(TickStage.GLOBAL_PHYSICS);
		}
	}

	private void doDynamicUpdates() {
		/*int passStartUpdates = updates.get() - 1;
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
		}*/
	}

	private void doLighting() {
        runTasks(TickStage.LIGHTING);
	}


	private void doFinalizeTick() {
        runTasks(TickStage.FINALIZE);
	}

	private void doCopySnapshot() {
        runTasks(TickStage.PRESNAPSHOT);
        runTasks(TickStage.SNAPSHOT);
	}

    public void runTasks(final TickStage stage) {
        TickStage.setStage(stage);
        Set<ManagerRunnable>[] sequences = managerRunnables[stage.getOrder() - 1];
        if (sequences == null) {
            return;
        }
        for (Set<ManagerRunnable> managers : sequences) {
            if (managers == null) {
                continue;
            }
            try {
                final List<Future<Void>> futures = executorService.invokeAll(managers);
                // invokeAll means that it returns when all futures are done or cancelled
                // We only want to report the exceptions, we don't want to wait
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < futures.size(); i++) {
                            try {
                                futures.get(i).get();
                            } catch (ExecutionException e) {
                                Spout.warn("Exception thrown when executing a task in tick stage " + stage, e);
                            } catch (InterruptedException e) {
                                Spout.warn("Interrupted when getting future", e);
                            }
                        }
                    }
                });
            } catch (InterruptedException e) {
                Spout.warn("Main thread interrupted while waiting on tick stage " + stage);
            }
        }
    }

	/**
	 * Adds an async manager to the scheduler
	 */
    @SuppressWarnings("unchecked")
	public void addAsyncManager(AsyncManager manager) {
        for (int stage = 0; stage < managerRunnableFactories.length; stage++) {
            ManagerRunnableFactory taskFactory = managerRunnableFactories[stage];
            if (taskFactory == null) {
                continue;
            }
            final int maxSequence = taskFactory.getMaxSequence();
            final int minSequence = taskFactory.getMinSequence();
            final int numSequences = maxSequence - minSequence + 1;
            Set<ManagerRunnable>[] sequences = managerRunnables[stage];
            if (sequences == null) {
                sequences = managerRunnables[stage] = new Set[numSequences];
            }
            if (((manager.getTickStages().getMask() >> stage) & 1) == 0) {
                continue;
            }
            for (int s = minSequence; s <= maxSequence; s++) {
                if (manager.checkSequence(taskFactory.getTickStage(), s)) {
                    Set<ManagerRunnable> sequence = sequences[s - minSequence];
                    if (sequence == null) {
                        sequences[s - minSequence] = sequence = new HashSet<>();
                    }
                    sequence.add(taskFactory.getTask(manager, s));
                }
            }
        }
	}

	/**
	 * Removes an async manager from the scheduler
	 */
	public void removeAsyncManager(AsyncManager manager) {
        for (int stage = 0; stage < managerRunnableFactories.length; stage++) {
            ManagerRunnableFactory taskFactory = managerRunnableFactories[stage];
            if (taskFactory == null) {
                continue;
            }
            final int maxSequence = taskFactory.getMaxSequence();
            final int minSequence = taskFactory.getMinSequence();
            Set<ManagerRunnable>[] sequences = managerRunnables[stage];
            if (sequences == null) {
                continue;
            }
            if (((manager.getTickStages().getMask() >> stage) & 1) == 0) {
                continue;
            }
            for (int s = minSequence; s <= maxSequence; s++) {
                Set<ManagerRunnable> sequence = sequences[s - minSequence];
                if (sequence != null) {
                    sequence.remove(taskFactory.getTask(manager, s));
                }
            }
        }
	}

    public int getTPS() {
        return tpsMonitor.getTPS();
    }
}
