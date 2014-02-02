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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.flowpowered.commons.datatable.ManagedHashMap;
import com.flowpowered.commons.datatable.SerializableMap;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.flowpowered.api.component.Component;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.EntitySnapshot;
import com.flowpowered.api.geo.discrete.Transform;

public class FlowEntitySnapshot implements EntitySnapshot {
    private final WeakReference<Entity> entity;
    private final int entityId;
    private final UUID uniqueId;
    private final Transform location;
    private final String worldName;
    private final UUID worldId;
    private final SerializableMap dataMap;
    private final boolean savable;
    private final List<Class<? extends Component>> components;
    private final long time = System.currentTimeMillis();

    public FlowEntitySnapshot(Entity e) {
        if (e.isRemoved()) {
            throw new IllegalArgumentException("Can not take a snapshot of a removed entity");
        }

        this.entity = new WeakReference<>(e);
        this.entityId = e.getId();
        this.uniqueId = e.getUID();
        //this.location = e.getPhysics().getTransform();
        this.location = null;
        this.worldName = e.getWorld().getName();
        this.worldId = e.getWorld().getUID();
        this.savable = e.isSavable();
        if (e.getData().size() > 0) {
            this.dataMap = e.getData().deepCopy();
        } else {
            this.dataMap = new ManagedHashMap();
        }
        components = new ArrayList<>();
        for (Component c : e.values()) {
            if (c.isDetachable()) {
                this.components.add(c.getClass());
            }
        }
    }

    public FlowEntitySnapshot(UUID id, Transform t, UUID worldId, byte[] dataMap, List<Class<? extends Component>> types) {
        this.entity = new WeakReference<>(null);
        this.entityId = -1;
        this.uniqueId = id;
        this.location = t;
        this.worldName = null;
        this.worldId = worldId;
        this.savable = true;
        this.dataMap = new ManagedHashMap();
        if (dataMap != null) {
            try {
                this.dataMap.deserialize(dataMap);
            } catch (IOException e) {
                throw new RuntimeException("Unable to deserialize data", e);
            }
        }
        this.components = new ArrayList<>(types);
    }

    @Override
    public Entity getReference() {
        return entity.get();
    }

    @Override
    public final int getId() {
        return entityId;
    }

    @Override
    public final UUID getUID() {
        return uniqueId;
    }

    @Override
    public final Transform getTransform() {
        return location;
    }

    @Override
    public final UUID getWorldUID() {
        return worldId;
    }

    @Override
    public String getWorldName() {
        return worldName;
    }

    @Override
    public final SerializableMap getDataMap() {
        return dataMap;
    }

    @Override
    public boolean isSavable() {
        return savable;
    }

    @Override
    public List<Class<? extends Component>> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public long getSnapshotTime() {
        return time;
    }
}
