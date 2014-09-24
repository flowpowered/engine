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


import com.flowpowered.api.Server;
import com.flowpowered.api.player.Player;
import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.commons.bit.ShortBitMask;
import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.engine.geo.region.FlowRegion;
import com.flowpowered.engine.geo.region.RegionGenerator;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.player.FlowPlayer;
import org.apache.logging.log4j.Logger;

public class WorldThread extends TickingElement {
    private final FlowScheduler scheduler;
    private final Logger logger;
    private final TPSMonitor tpsMonitor = new TPSMonitor();
    private WorldTickStage currentStage = WorldTickStage.STAGE1;
    private final FlowWorld world;

    public WorldThread(FlowScheduler scheduler, FlowWorld world) {
        super("WorldThread - " + world.getName(), 20);
        this.world = world;
        this.scheduler = scheduler;
        this.logger = scheduler.getEngine().getLogger();
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

    @Override
    public void onTick(long delta) {
        // Delta is in nanos, we want millis with rounding
        final long realDelta = Math.round(delta * 1e-6d);

        currentStage = WorldTickStage.TICKSTART;

        scheduler.getTaskManager().heartbeat(realDelta);

        world.startTickRun(0, realDelta);
        world.startTickRun(1, realDelta);

        doFinalizeTick();

        doCopySnapshot();

        Server server = scheduler.getEngine().get(Server.class);
        if (server != null) {
            for (Player p : server.getOnlinePlayers()) {
                ((FlowPlayer) p).copyInput(world);
            }
        }

        tpsMonitor.update();
    }

    private void doFinalizeTick() {
        world.finalizeRun();
    }

    private void doCopySnapshot() {
        world.preSnapshotRun();

        world.getRegions().stream().map((r) -> (FlowRegion) r).forEach(FlowRegion::copySnapshotRun);
        world.copySnapshotRun();
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