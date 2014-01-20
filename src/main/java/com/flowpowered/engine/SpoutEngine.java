package com.flowpowered.engine;

import com.flowpowered.events.EventManager;
import com.flowpowered.events.SimpleEventManager;

import com.flowpowered.api.Engine;
import com.flowpowered.api.material.MaterialRegistry;
import com.flowpowered.api.util.SyncedStringMap;
import com.flowpowered.engine.filesystem.SpoutFileSystem;
import com.flowpowered.engine.scheduler.SpoutScheduler;
import com.flowpowered.engine.util.thread.snapshotable.SnapshotManager;

public abstract class SpoutEngine implements Engine {
    private final SpoutApplication args;
    private final EventManager eventManager;
    private final SpoutFileSystem fileSystem;

    private SpoutScheduler scheduler;
    protected final SnapshotManager snapshotManager = new SnapshotManager();
    private SyncedStringMap itemMap;


    public SpoutEngine(SpoutApplication args) {
        this.args = args;
        this.eventManager = new SimpleEventManager();
        this.fileSystem = new SpoutFileSystem();
    }

	@Override
	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

    public void init() {
        itemMap = MaterialRegistry.setupRegistry();
        scheduler = new SpoutScheduler(this);
    }

    public void start() {
        scheduler.startMainThread();
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
    public SpoutScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public SpoutFileSystem getFileSystem() {
        return fileSystem;
    }

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public String getName() {
        return "Spout Engine";
    }

    public SnapshotManager getSnapshotManager() {
        return snapshotManager;
    }
}
