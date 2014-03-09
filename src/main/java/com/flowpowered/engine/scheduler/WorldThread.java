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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.flowpowered.api.player.Player;
import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.commons.bit.ShortBitMask;
import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.engine.FlowServer;
import com.flowpowered.engine.geo.region.RegionGenerator;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.player.FlowPlayer;
import com.flowpowered.engine.util.thread.AsyncManager;
import com.flowpowered.engine.util.thread.LoggingThreadPoolExecutor;
import com.flowpowered.engine.util.thread.coretasks.CopySnapshotTask;
import com.flowpowered.engine.util.thread.coretasks.FinalizeTask;
import com.flowpowered.engine.util.thread.coretasks.GlobalDynamicUpdatesTask;
import com.flowpowered.engine.util.thread.coretasks.GlobalPhysicsTask;
import com.flowpowered.engine.util.thread.coretasks.LightingTask;
import com.flowpowered.engine.util.thread.coretasks.LocalDynamicUpdatesTask;
import com.flowpowered.engine.util.thread.coretasks.LocalPhysicsTask;
import com.flowpowered.engine.util.thread.coretasks.ManagerRunnable;
import com.flowpowered.engine.util.thread.coretasks.ManagerRunnableFactory;
import com.flowpowered.engine.util.thread.coretasks.PreSnapshotTask;
import com.flowpowered.engine.util.thread.coretasks.StartTickTask;
import org.apache.logging.log4j.Logger;

public class WorldThread extends TickingElement {
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
    private final FlowScheduler scheduler;
    private final Logger logger;

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
    protected final ExecutorService executorService;
    private final TPSMonitor tpsMonitor = new TPSMonitor();
    private WorldTickStage currentStage = WorldTickStage.STAGE1;
    private FlowWorld world;

    public WorldThread(FlowScheduler scheduler, FlowWorld world) {
        super("WorldThread - " + world.getName(), 20);
        this.world = world;
        this.scheduler = scheduler;
        this.logger = scheduler.getEngine().getLogger();
        final int nThreads = Runtime.getRuntime().availableProcessors();
        executorService = LoggingThreadPoolExecutor.newFixedThreadExecutorWithMarkedName(nThreads, "WorldThread - AsyncManager executor service", scheduler.getEngine().getLogger());
    }

    @Override
    public void onStart() {
        tpsMonitor.start();
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

        currentStage = WorldTickStage.TICKSTART;

        scheduler.getTaskManager().heartbeat(delta);

        runTasks(WorldTickStage.STAGE1);

        runTasks(WorldTickStage.STAGE2P);

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

        if (scheduler.getEngine().getPlatform().isServer()) {
            for (Player p : ((FlowServer) scheduler.getEngine()).getOnlinePlayers()) {
                ((FlowPlayer) p).copyInput(world);
            }
        }

        updateManagers();

        tpsMonitor.update();
    }


    private void doPhysics() {
        int passStartUpdates = updates.get() - 1;
        int startUpdates = updates.get();
        while (passStartUpdates < updates.get() && updates.get() < startUpdates + UPDATE_THRESHOLD) {
            passStartUpdates = updates.get();
            runTasks(WorldTickStage.PHYSICS);
            runTasks(WorldTickStage.GLOBAL_PHYSICS);
        }
    }

    private void doDynamicUpdates() {
        /*int passStartUpdates = updates.get() - 1;
        int startUpdates = updates.get();

        WorldTickStage.setStage(WorldTickStage.GLOBAL_DYNAMIC_BLOCKS);

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

            this.runTasks(managers, dynamicUpdatesTask, "Dynamic Blocks", WorldTickStage.GLOBAL_DYNAMIC_BLOCKS, WorldTickStage.DYNAMIC_BLOCKS);
        }*/
    }

    private void doLighting() {
        runTasks(WorldTickStage.LIGHTING);
    }


    private void doFinalizeTick() {
        runTasks(WorldTickStage.FINALIZE);
    }

    private void doCopySnapshot() {
        runTasks(WorldTickStage.PRESNAPSHOT);
        runTasks(WorldTickStage.SNAPSHOT);
    }

    public void runTasks(final WorldTickStage stage) {
        currentStage = stage;
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
            } catch (InterruptedException e) {
                logger.warn("Main thread interrupted while waiting on tick stage " + stage);
            }
        }
    }

    /**
     * Adds an async manager to the scheduler
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
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

    public WorldTickStage getStage() {
        return currentStage;
    }

    /**
     * Checks if the current stages is one of the valid allowed stages.
     *
     * @param allowedStages the OR of all the allowed stages
     */
    public void checkStage(ShortBitMask allowedStages) {
        if (!testStage(allowedStages)) {
            throw new IllegalWorldTickSequenceException(allowedStages.getMask(), currentStage);
        }
    }

    /**
     * Checks if the current currentStages is one of the valid allowed currentStages, but does not throw an exception.
     *
     * @param allowedStages the OR of all the allowed currentStages
     * @return true if the current currentStage is one of the allowed currentStages
     */
    public boolean testStage(ShortBitMask allowedStages) {
        return (currentStage.getMask() & allowedStages.getMask()) != 0;
    }

    /**
     * Checks if the current thread is the owner thread and the current currentStage is one of the restricted currentStages, or that the current currentStage is one of the open currentStages
     *
     * @param allowedStages the OR of all the open currentStages
     * @param restrictedStages the OR of all restricted currentStages
     * @param ownerThread the thread that has restricted access
     */
    public void checkStage(ShortBitMask allowedStages, ShortBitMask restrictedStages, Thread ownerThread) {
        if ((currentStage.getMask() & allowedStages.getMask()) != 0 && ((Thread.currentThread() != ownerThread || (currentStage.getMask() & restrictedStages.getMask()) == 0))) {
            throw new IllegalWorldTickSequenceException(allowedStages.getMask(), restrictedStages.getMask(), ownerThread, currentStage);
        }
    }

    /**
     * Checks if the current thread is the owner thread and the current currentStage is one of the restricted currentStages
     *
     * @param restrictedStages the OR of all restricted currentStages
     * @param ownerThread the thread that has restricted access
     */
    public void checkStage(ShortBitMask restrictedStages, Thread ownerThread) {
        if (((currentStage.getMask() & restrictedStages.getMask()) == 0) || Thread.currentThread() != ownerThread) {
            throw new IllegalWorldTickSequenceException(restrictedStages.getMask(), 0, ownerThread, currentStage);
        }
    }
}