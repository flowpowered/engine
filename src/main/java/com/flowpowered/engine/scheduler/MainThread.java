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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Logger;

import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.api.Flow;
import com.flowpowered.api.input.InputSnapshot;
import com.flowpowered.api.Server;
import com.flowpowered.api.player.ClientPlayer;
import com.flowpowered.api.player.Player;
import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.engine.FlowClient;
import com.flowpowered.engine.player.FlowPlayer;
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

    @SuppressWarnings("unchecked")
    private final Set<ManagerRunnable>[][] managerRunnables = new Set[managerRunnableFactories.length][];
    // We are going to synchronize on this, don't need concurrent
    private final List<ManagerRunnableUpdateEntry> manageRunnableUpdates = new LinkedList<>();

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

        if (scheduler.getInputThread() != null) {
            scheduler.getInputThread().subscribeToInput();
        }
        updateManagers();
    }

    @Override
    public void onStop() {
        doCopySnapshot();
        RegionGenerator.shutdownExecutorService();
        RegionGenerator.awaitExecutorServiceTermination(logger);

        executorService.shutdown();

        scheduler.getTaskManager().heartbeat(FlowScheduler.PULSE_EVERY << 2);
        scheduler.getTaskManager().stop();

        long delay = 2000;
        while (!scheduler.getTaskManager().waitForAsyncTasks(delay)) {
            logger.info("Unable to shutdown due to async tasks still running");
            if (delay < 8000) {
                delay <<= 1;
            }
        }

        if (scheduler.getInputThread() != null) {
            scheduler.getInputThread().unsubscribeToInput();
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

        if (Flow.getEngine().getPlatform().isServer()) {
            for (Player p : ((Server) Flow.getEngine()).getOnlinePlayers()) {
                ((FlowPlayer) p).getNetwork().finalizeRun();
                ((FlowPlayer) p).getNetwork().preSnapshotRun();
            }
        } else {
            ClientPlayer player = ((FlowClient) scheduler.getEngine()).getPlayer();
            if (player != null) {
                player.getNetwork().finalizeRun();
                player.getNetwork().preSnapshotRun();
            }
        }

        doCopySnapshot();
        updateManagers();

        // TEST CODE
        if (scheduler.getInputThread().isActive()) {
            List<InputSnapshot> inputList = new ArrayList<>();
            Queue<InputSnapshot> inputQueue = scheduler.getInputThread().getInputQueue();
            while (!inputQueue.isEmpty()) {
                InputSnapshot current = inputQueue.poll();
                inputList.add(current);
            }
            if (Flow.getEngine().getPlatform().isClient()) {
                ((FlowClient) Flow.getEngine()).getPlayer().setInput(inputList);
            }
        }
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
                // TODO: modify LoggingThreadPoolExecutor to allow custom logging
                /*
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
                */
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
                        // We can use HashSet because we're never going to modify this concurrently
                        sequences[s - minSequence] = sequence = new HashSet<>();
                    }
                    synchronized (manageRunnableUpdates) {
                        manageRunnableUpdates.add(new ManagerRunnableUpdateEntry(stage, s - minSequence, taskFactory.getTask(manager, s), true));
                    }
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
                    synchronized (manageRunnableUpdates) {
                        manageRunnableUpdates.add(new ManagerRunnableUpdateEntry(stage, s - minSequence, taskFactory.getTask(manager, s), false));
                    }
                }
            }
        }
    }

    private void updateManagers() {
        // Small and seldom possibility that there are sync updates to this that might conflict: use synchronized
        synchronized (manageRunnableUpdates) {
            if (manageRunnableUpdates.isEmpty()) {
                return;
            }
            for (ManagerRunnableUpdateEntry e : manageRunnableUpdates) {
                if (e.add) {
                    managerRunnables[e.stage][e.sequence].add(e.runnable);
                } else {
                    managerRunnables[e.stage][e.sequence].remove(e.runnable);
                }
            }
            manageRunnableUpdates.clear();
        }
    }

    private static class ManagerRunnableUpdateEntry {
        private final int stage, sequence;
        private final ManagerRunnable runnable;
        private final boolean add;

        private ManagerRunnableUpdateEntry(int stage, int sequence, ManagerRunnable runnable, boolean add) {
            this.stage = stage;
            this.sequence = sequence;
            this.runnable = runnable;
            this.add = add;
        }
    }

    public int getTPS() {
        return tpsMonitor.getTPS();
    }
}
