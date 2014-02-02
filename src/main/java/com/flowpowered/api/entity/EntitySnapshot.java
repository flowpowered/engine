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

import java.util.List;
import java.util.UUID;

import com.flowpowered.commons.datatable.SerializableMap;

import com.flowpowered.api.component.Component;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.scheduler.Snapshot;

/**
 * Represents a snapshot of an entity state at a specific UTC timestamp, with immutable values
 */
public interface EntitySnapshot extends Snapshot<Entity> {

    /**
     * Gets the id of the entity. <p> Entity ids' may become invalid if the server has stopped and started. They do not persist across server instances. For persistent ids, use {@link #getUID()}. </p>
     *
     * @return id
     */
    public int getId();

    /**
     * Gets the UID for the entity. <p> This id is persistent across server instances, unique to this entity </p>
     *
     * @return uid
     */
    public UUID getUID();

    /**
     * Gets the transform for the entity. <p> Note: if the world that the entity was in has been unloaded, the world in the transform will be null. </p>
     *
     * @return transform
     */
    public Transform getTransform();

    /**
     * Gets the UUID of the world that the entity was in at the time of this snapshot
     *
     * @return uid
     */
    public UUID getWorldUID();

    /**
     * Gets the name of the world that the entity was in at the time of this snapshot
     *
     * @return world name
     */
    public String getWorldName();

    /**
     * Gets a copy of the data map for the entity, created at the time of this snapshot
     *
     * @return data map
     */
    public SerializableMap getDataMap();

    /**
     * Gets the savable flag for the entity at the time of the snapshot
     *
     * @return savable
     */
    public boolean isSavable();

    /**
     * Gets a list of the classes of components attached to this entity
     *
     * @return entity
     */
    public List<Class<? extends Component>> getComponents();
}
