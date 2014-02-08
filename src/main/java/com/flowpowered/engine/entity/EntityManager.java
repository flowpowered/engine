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
package com.flowpowered.engine.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.engine.FlowEngine;
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
    private final SnapshotableHashMap<Integer, FlowEntity> entities = new SnapshotableHashMap<>(snapshotManager);
    /**
     * The next id to check.
     */
    private final static AtomicInteger nextId = new AtomicInteger(1);
    /**
     * Player listings plus listings of sync'd entities per player
     */
    private final SnapshotableHashMap<Player, ArrayList<FlowEntity>> players = new SnapshotableHashMap<>(snapshotManager);

    /**
     * Gets all entities.
     *
     * @return A collection of entities.
     */
    public Collection<FlowEntity> getAll() {
        return entities.get().values();
    }

    /**
     * Gets all the entities that are in a live state (not the snapshot).
     *
     * @return A collection of entities
     */
    public Collection<FlowEntity> getAllLive() {
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
    public FlowEntity getEntity(int id) {
        return entities.get().get(id);
    }

    /**
     * Adds an entity to the manager.
     *
     * @param entity The entity
     */
    public void addEntity(FlowEntity entity) {
        entities.put(entity.getId(), entity);
        if (entity instanceof Player) {
            players.put((Player) entity, new ArrayList<FlowEntity>());
        }
    }

    public static FlowEntity createEntity(FlowEngine engine, Transform transform) {
        return new FlowEntity(engine, getNextId(), transform);
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
    public void removeEntity(FlowEntity entity) {
        entities.remove(entity.getId());
        if (entity instanceof Player) {
            players.remove((Player) entity);
        }
    }

    /**
     * Finalizes the manager at the FINALIZERUN tick stage
     */
    public void finalizeRun() {
        for (FlowEntity e : entities.get().values()) {
            e.finalizeRun();
        }
    }

    /**
     * Finalizes the manager at the FINALIZERUN tick stage
     */
    public void preSnapshotRun() {
        for (FlowEntity e : entities.get().values()) {
            e.preSnapshotRun();
        }
    }

    /**
     * Snapshots the manager and all the entities managed in the SNAPSHOT tickstage.
     */
    public void copyAllSnapshots() {
        for (FlowEntity e : entities.get().values()) {
            e.copySnapshot();
        }
        snapshotManager.copyAllSnapshots();

        // We want one more tick with for the removed Entities
        // The next tick works with the snapshotted values which contains has all removed entities with isRemoved true
        for (FlowEntity e : entities.get().values()) {
            if (e.isRemoved()) {
                removeEntity(e);
            }
        }
    }
}
