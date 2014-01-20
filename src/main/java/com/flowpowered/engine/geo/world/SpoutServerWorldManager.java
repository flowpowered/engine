package com.flowpowered.engine.geo.world;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.flowpowered.commons.StringUtil;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import com.flowpowered.api.Server;
import com.flowpowered.api.generator.EmptyWorldGenerator;
import com.flowpowered.api.generator.WorldGenerator;
import com.flowpowered.api.geo.ServerWorld;
import com.flowpowered.api.geo.ServerWorldManager;
import com.flowpowered.api.geo.World;
import com.flowpowered.engine.SpoutEngine;
import com.flowpowered.engine.SpoutServer;
import com.flowpowered.engine.filesystem.SpoutFileSystem;
import com.flowpowered.engine.filesystem.WorldFiles;


public class SpoutServerWorldManager extends SpoutWorldManager<SpoutServerWorld> implements ServerWorldManager {
	private final WorldGenerator defaultGenerator = new EmptyWorldGenerator();

    public <E extends SpoutEngine & Server> SpoutServerWorldManager(E engine) {
        super(engine);
    }

	@Override
	public Collection<File> matchWorldFolder(String worldName) {
		return StringUtil.matchFile(getWorldFolders(), worldName);
	}

	@Override
	public SpoutServerWorld loadWorld(String name, WorldGenerator generator) {
		if (loadedWorlds.get().containsKey((name))) {
			return loadedWorlds.get().get(name);
		}
		if (loadedWorlds.getLive().containsKey(name)) {
			return loadedWorlds.getLive().get(name);
		}

		if (generator == null) {
			generator = defaultGenerator;
		}

		SpoutServerWorld world = WorldFiles.loadWorld((SpoutServer) engine, generator, name);

		SpoutServerWorld oldWorld = loadedWorlds.putIfAbsent(name, world);

		if (oldWorld != null) {
			return oldWorld;
		}

		engine.getScheduler().addAsyncManager(world);
		//getEventManager().callDelayedEvent(new WorldLoadEvent(world));
		return world;
	}

	@Override
	public void save(boolean worlds, boolean players) {
		// TODO: Auto-generated method stub
	}

	@Override
	public List<File> getWorldFolders() {
		File[] folders = this.getWorldFolder().listFiles((FilenameFilter) DirectoryFileFilter.INSTANCE);
		if (folders == null || folders.length == 0) {
			return new ArrayList<>();
		}
		List<File> worlds = new ArrayList<>(folders.length);
		// Are they really world folders?
		for (File world : folders) {
			if (new File(world, "world.dat").exists()) {
				worlds.add(world);
			}
		}
		return worlds;
	}

	@Override
	public File getWorldFolder() {
		return SpoutFileSystem.WORLDS_DIRECTORY;
	}

	@Override
	public WorldGenerator getDefaultGenerator() {
		return defaultGenerator;
	}

	@Override
	public boolean unloadWorld(String name, boolean save) {
		return unloadWorld((ServerWorld) loadedWorlds.getLive().get(name), save);
	}

	@Override
	public boolean unloadWorld(ServerWorld world, boolean save) {
		if (world == null) {
			return false;
		}

        SpoutServerWorld w = (SpoutServerWorld) world;
		boolean success = loadedWorlds.remove(world.getName(), w);
		if (success) {
			if (save) {
				engine.getScheduler().removeAsyncManager(w);
				//getEventManager().callDelayedEvent(new WorldUnloadEvent(world));
				w.unload(save);
			}
			// Note: Worlds should not allow being saved twice and/or throw exceptions if accessed after unloading.
			// Also, should blank out as much internal world data as possible, in case plugins retain references to unloaded worlds.
		}
		return success;
	}
}
