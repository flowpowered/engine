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
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.lidalia.slf4jext.Level;

import com.flowpowered.commons.LoggerOutputStream;

import com.flowpowered.events.EventManager;
import com.flowpowered.events.SimpleEventManager;

import com.flowpowered.api.EnginePart;
import com.flowpowered.api.event.engine.EnginePartAddedEvent;
import com.flowpowered.api.util.LogUtil;

import com.flowpowered.engine.filesystem.FlowFileSystem;
import com.flowpowered.engine.geo.world.FlowWorldManager;
import com.flowpowered.engine.plugins.FlowPluginManager;
import com.flowpowered.engine.scheduler.FlowScheduler;

public class FlowEngineImpl implements FlowEngine {
    private boolean debug;
    private Set<FlowEnginePart> parts = new HashSet<>();
    private final EventManager eventManager;
    private final FlowFileSystem fileSystem;
    private final FlowPluginManager pluginManager;
    private FlowScheduler scheduler;
    private final FlowWorldManager worldManager;
    private final Logger logger = LogManager.getLogger("Flow");

    public FlowEngineImpl() {
        this.worldManager = new FlowWorldManager(this);
        this.eventManager = new SimpleEventManager();
        this.fileSystem = new FlowFileSystem();
        this.pluginManager = new FlowPluginManager(LogUtil.toSLF(logger), this);
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public void init(boolean debugMode) {
        this.debug = debugMode;
        // Make sure we log something and let log4j2 initialize before we redirect System.out and System.err
        // Otherwise it could try to log to the redirected stdout causing infinite loop.
        logger.info("Initializing Engine.");
        //And now redirect the streams to a logger.
        String loggerName = logger.getName();
        System.setOut(new PrintStream(new LoggerOutputStream(LoggerFactory.getLogger(loggerName + ".STDOUT"), Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(LoggerFactory.getLogger(loggerName + ".STDERR"), Level.WARN), true));
        scheduler = new FlowScheduler(this);
    }

    public void start() {
        pluginManager.loadPlugins();
        pluginManager.enablePlugins();
        scheduler.startMainThread();
        System.out.println("Engine started.");
    }

    @Override
    public boolean stop(String reason) {
        scheduler.stop();
        pluginManager.disablePlugins();;
        System.out.println("Engine stopped");
        return true;
    }

    @Override
    public boolean debugMode() {
        return debug;
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
    public FlowPluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public FlowWorldManager getWorldManager() {
        return worldManager;
    }

    @Override
    public <P extends EnginePart> P get(Class<P> part) {
        for (FlowEnginePart p : parts) {
            if (part.isInstance(p)) {
                return (P) p;
            }
        }
        return null;
    }

    public <P extends FlowEnginePart> void add(P part) {
        parts.add(part);
        part.onAdd();
        eventManager.callEvent(new EnginePartAddedEvent(part));
    }
}
