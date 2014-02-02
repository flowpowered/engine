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
package com.flowpowered.api.entity;

import java.util.UUID;

import com.flowpowered.commons.datatable.ManagedMap;

import com.flowpowered.api.Engine;
import com.flowpowered.api.component.ComponentOwner;
import com.flowpowered.api.geo.WorldSource;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.scheduler.tickable.Tickable;

/**
 * Represents an entity, which may or may not be spawned into the world.
 */
public interface Entity extends Tickable, WorldSource, ComponentOwner {
    /**
     * Gets the current ID of this entity within the current game session
     *
     * @return The entities' id.
     */
    public int getId();

    /**
     * Gets the entity's persistent unique id. <p> Can be used to look up the entity and persists between starts.
     *
     * @return persistent {@link UUID}
     */
    public UUID getUID();

    /**
     * Gets the {@link Engine} that spawned and is managing this entity
     *
     * @return {@link Engine}
     */
    public Engine getEngine();

    /**
     * Removes the entity. This takes effect at the next snapshot.
     */
    public void remove();

    /**
     * True if the entity is removed.
     *
     * @return removed
     */
    public boolean isRemoved();

    /**
     * Sets whether or not the entity should be saved.<br/>
     *
     * @param savable True if the entity should be saved, false if not
     */
    public void setSavable(boolean savable);

    /**
     * Returns true if this entity should be saved.
     *
     * @return savable
     */
    public boolean isSavable();

    /**
     * Gets the {@link Chunk} this entity resides in, or null if removed.
     *
     * @return {@link Chunk} the entity is in, or null if removed.
     */
    public Chunk getChunk();

    /**
     * Gets the region the entity is associated and managed with, or null if removed.
     *
     * @return {@link Region} the entity is in.
     */
    public Region getRegion();

    /**
     * Creates an immutable snapshot of the entity state at the time the method is called
     *
     * @return immutable snapshot
     */
    public EntitySnapshot snapshot();

    public Physics getPhysics();

    /**
     * Gets the {@link ManagedMap} which an Entity always has.
     *
     * @return {@link ManagedMap}
     */
    public ManagedMap getData();
}
