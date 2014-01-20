/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package com.flowpowered.engine.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.engine.util.thread.snapshotable.SnapshotManager;
import com.flowpowered.engine.util.thread.snapshotable.SnapshotableHashMap;

/**
 * A class which manages all of the entities within a world.
 */
public class EntityManager {
	/**
	 * The snapshot manager
	 */
	protected final SnapshotManager snapshotManager = new SnapshotManager();
	/**
	 * A map of all the entity ids to the corresponding entities.
	 */
	private final SnapshotableHashMap<Integer, SpoutEntity> entities = new SnapshotableHashMap<>(snapshotManager);
	/**
	 * The next id to check.
	 */
	private final static AtomicInteger nextId = new AtomicInteger(1);
	/**
	 * Player listings plus listings of sync'd entities per player
	 */
	private final SnapshotableHashMap<Player, ArrayList<SpoutEntity>> players = new SnapshotableHashMap<>(snapshotManager);

	/**
	 * Gets all entities.
	 *
	 * @return A collection of entities.
	 */
	public Collection<SpoutEntity> getAll() {
		return entities.get().values();
	}

	/**
	 * Gets all the entities that are in a live state (not the snapshot).
	 *
	 * @return A collection of entities
	 */
	public Collection<SpoutEntity> getAllLive() {
		return entities.getLive().values();
	}

	/**
	 * Gets all the players currently in the engine.
	 *
	 * @return The list of players.
	 */
	public List<Player> getPlayers() {
		return new ArrayList<>(players.get().keySet());
	}

	/**
	 * Gets an entity by its id.
	 *
	 * @param id The id.
	 * @return The entity, or {@code null} if it could not be found.
	 */
	public SpoutEntity getEntity(int id) {
		return entities.get().get(id);
	}

	/**
	 * Adds an entity to the manager.
	 *
	 * @param entity The entity
	 */
	public void addEntity(SpoutEntity entity) {
		entities.put(entity.getId(), entity);
		if (entity instanceof Player) {
			players.put((Player) entity, new ArrayList<SpoutEntity>());
		}
	}

    public static SpoutEntity createEntity(Transform transform) {
        return new SpoutEntity(getNextId(), transform);
    }

	private static int getNextId() {
		int id = nextId.getAndIncrement();
		if (id == -2) {
			throw new IllegalStateException("Entity id space exhausted");
		}
		return id;
	}

	/**
	 * Removes an entity from the manager.
	 *
	 * @param entity The entity
	 */
	public void removeEntity(SpoutEntity entity) {
		entities.remove(entity.getId());
		if (entity instanceof Player) {
			players.remove((Player) entity);
		}
	}

	/**
	 * Finalizes the manager at the FINALIZERUN tick stage
	 */
	public void finalizeRun() {
		for (SpoutEntity e : entities.get().values()) {
			e.finalizeRun();
		}
	}

	/**
	 * Finalizes the manager at the FINALIZERUN tick stage
	 */
	public void preSnapshotRun() {
		for (SpoutEntity e : entities.get().values()) {
			e.preSnapshotRun();
		}
	}

	/**
	 * Snapshots the manager and all the entities managed in the SNAPSHOT tickstage.
	 */
	public void copyAllSnapshots() {
		for (SpoutEntity e : entities.get().values()) {
			e.copySnapshot();
		}
		snapshotManager.copyAllSnapshots();

		// We want one more tick with for the removed Entities
		// The next tick works with the snapshotted values which contains has all removed entities with isRemoved true
		for (SpoutEntity e : entities.get().values()) {
			if (e.isRemoved()) {
				removeEntity(e);
			}
		}
	}
}
