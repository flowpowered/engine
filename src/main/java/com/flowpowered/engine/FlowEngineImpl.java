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
package com.flowpowered.engine;

import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import com.flowpowered.commons.LoggerOutputStream;
import com.flowpowered.events.EventManager;
import com.flowpowered.events.SimpleEventManager;
import com.flowpowered.api.material.MaterialRegistry;
import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.api.util.SyncedStringMap;
import com.flowpowered.commons.bit.ShortBitMask;
import com.flowpowered.engine.filesystem.FlowFileSystem;
import com.flowpowered.engine.scheduler.FlowScheduler;
import com.flowpowered.engine.util.thread.CopySnapshotManager;
import com.flowpowered.engine.util.thread.snapshotable.SnapshotManager;

import uk.org.lidalia.slf4jext.Level;

public abstract class FlowEngineImpl implements FlowEngine, CopySnapshotManager {
    private final FlowApplication args;
    private final EventManager eventManager;
    private final FlowFileSystem fileSystem;
    private FlowScheduler scheduler;
    protected final SnapshotManager snapshotManager = new SnapshotManager();
    private SyncedStringMap itemMap;
    private PrintStream realSystemOut;
    private PrintStream realSystemErr;
    private final Logger logger = LogManager.getLogger("Flow");

    public FlowEngineImpl(FlowApplication args) {
        this.args = args;
        this.eventManager = new SimpleEventManager();
        this.fileSystem = new FlowFileSystem();
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public void init() {
        // Make sure we log something and let log4j2 initialize before we redirect System.out and System.err
        // Otherwise it could try to log to the redirected stdout causing infinite loop.
        logger.info("Initializing Engine.");
        // Just in case.
        realSystemOut = System.out;
        realSystemErr = System.err;
        //And now redirect the streams to a logger.
        String loggerName = logger.getName();
        System.setOut(new PrintStream(new LoggerOutputStream(LoggerFactory.getLogger(loggerName + ".STDOUT"), Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(LoggerFactory.getLogger(loggerName + ".STDERR"), Level.WARN), true));
        itemMap = MaterialRegistry.setupRegistry();
        scheduler = new FlowScheduler(this);
    }

    public void start() {
        scheduler.startMainThread();
        scheduler.addAsyncManager(this);
        System.out.println("Engine started.");
    }

    @Override
    public boolean stop() {
        scheduler.stop();
        System.out.println("Engine stopped");
        return true;
    }

    @Override
    public boolean stop(String reason) {
        return stop();
    }

    @Override
    public boolean debugMode() {
        return args.debug;
    }

    @Override
    public FlowScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public FlowFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public String getName() {
        return "Flow Engine";
    }

    @Override
    public SnapshotManager getSnapshotManager() {
        return snapshotManager;
    }

    @Override
    public void copySnapshotRun(int sequence) {
        snapshotManager.copyAllSnapshots();
    }

    @Override
    public boolean checkSequence(TickStage stage, int sequence) {
        switch (stage) {
            case SNAPSHOT:
                return sequence == 0;
        }
        return true;
    }

    @Override
    public Thread getExecutionThread() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExecutionThread(Thread t) {
    }

    private final ShortBitMask STAGES = TickStage.SNAPSHOT;
    @Override
    public ShortBitMask getTickStages() {
        return STAGES;
    }

}
