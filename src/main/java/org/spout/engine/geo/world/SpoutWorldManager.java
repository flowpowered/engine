package org.spout.engine.geo.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.flowpowered.commons.StringUtil;

import org.spout.api.geo.World;
import org.spout.api.geo.WorldManager;
import org.spout.engine.SpoutEngine;
import org.spout.engine.util.thread.snapshotable.SnapshotableLinkedHashMap;

public class SpoutWorldManager<T extends SpoutWorld> implements WorldManager {
    protected final SpoutEngine engine;
	protected final SnapshotableLinkedHashMap<String, T> loadedWorlds;

    public SpoutWorldManager(SpoutEngine engine) {
        loadedWorlds = new SnapshotableLinkedHashMap<>(engine.getSnapshotManager());
        this.engine = engine;
    }

    @Override
    public World getWorld(String name) {
        return getWorld(name, true);
    }

    @Override
    public World getWorld(String name, boolean exact) {
		if (exact) {
			SpoutWorld world = loadedWorlds.get().get(name);
			if (world != null) {
				return world;
			}
			return loadedWorlds.get().get(name);
		} else {
			return StringUtil.getShortest(StringUtil.matchName(loadedWorlds.getValues(), name));
		}
    }

	@Override
	public Collection<World> matchWorld(String name) {
		return StringUtil.matchName(getWorlds(), name);
	}

	@Override
	public SpoutWorld getWorld(UUID uid) {
		for (SpoutWorld world : loadedWorlds.getValues()) {
			if (world.getUID().equals(uid)) {
				return world;
			}
		}
		return null;
	}

    @Override
    public Collection<World> getWorlds() {
		Collection<World> w = new ArrayList<>();
		for (SpoutWorld world : loadedWorlds.getValues()) {
			w.add(world);
		}
		return w;
    }

}
