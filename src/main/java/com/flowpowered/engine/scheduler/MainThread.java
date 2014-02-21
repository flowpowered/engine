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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Logger;

import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.engine.geo.region.RegionGenerator;
import com.flowpowered.engine.util.thread.AsyncManager;
import com.flowpowered.engine.util.thread.LoggingThreadPoolExecutor;
import com.flowpowered.engine.util.thread.coretasks.CopySnapshotTask;
import com.flowpowered.engine.util.thread.coretasks.LocalDynamicUpdatesTask;
import com.flowpowered.engine.util.thread.coretasks.GlobalDynamicUpdatesTask;
import com.flowpowered.engine.util.thread.coretasks.FinalizeTask;
import com.flowpowered.engine.util.thread.coretasks.LightingTask;
import com.flowpowered.engine.util.thread.coretasks.ManagerRunnable;
import com.flowpowered.engine.util.thread.coretasks.ManagerRunnableFactory;
import com.flowpowered.engine.util.thread.coretasks.LocalPhysicsTask;
import com.flowpowered.engine.util.thread.coretasks.GlobalPhysicsTask;
import com.flowpowered.engine.util.thread.coretasks.PreSnapshotTask;
import com.flowpowered.engine.util.thread.coretasks.StartTickTask;

public class MainThread extends TickingElement {
    private final FlowScheduler scheduler;
    private final Logger logger;
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

    public MainThread(FlowScheduler scheduler) {
        super("MainThread", 20);
        this.scheduler = scheduler;
        this.logger = scheduler.getEngine().getLogger();
        final int nThreads = Runtime.getRuntime().availableProcessors() * 2 + 1;
        executorService = LoggingThreadPoolExecutor.newFixedThreadExecutorWithMarkedName(nThreads, "FlowScheduler - AsyncManager executor service", scheduler.getEngine().getLogger());
    }

    @Override
    public void onStart() {
        tpsMonitor.start();
    }

    @Override
    public void onStop() {
        doCopySnapshot();
        RegionGenerator.shutdownExecutorService();
        RegionGenerator.awaitExecutorServiceTermination(logger);

        scheduler.getTaskManager().heartbeat(FlowScheduler.PULSE_EVERY << 2);
        scheduler.getTaskManager().stop();

        long delay = 2000;
        while (!scheduler.getTaskManager().waitForAsyncTasks(delay)) {
            logger.info("Unable to shutdown due to async tasks still running");
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
            logger.warn("Block updates per tick of " + totalUpdates + " exceeded the threshold " + UPDATE_THRESHOLD + "; " + dynamicUpdates + " dynamic updates, " + physicsUpdates + " block physics updates and " + lightUpdates + " lighting updates");
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

        long earliestTime = FlowScheduler.END_OF_THE_WORLD;

        for (AsyncManager e : managers) {
            long firstTime = e.getFirstDynamicUpdateTime();
            if (firstTime < earliestTime) {
                earliestTime = firstTime;
            }
        }

        while (passStartUpdates < updates.get() && updates.get() < startUpdates + UPDATE_THRESHOLD) {
            passStartUpdates = updates.get();

            long threshold = earliestTime + FlowScheduler.PULSE_EVERY - 1;

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
                                logger.warn("Exception thrown when executing a task in tick stage " + stage, e);
                            } catch (InterruptedException e) {
                                logger.warn("Interrupted when getting future", e);
                            }
                        }
                    }
                });
            } catch (InterruptedException e) {
                logger.warn("Main thread interrupted while waiting on tick stage " + stage);
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
