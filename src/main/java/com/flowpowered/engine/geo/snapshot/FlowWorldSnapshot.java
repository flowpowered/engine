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
package com.flowpowered.engine.geo.snapshot;

import com.flowpowered.api.geo.snapshot.WorldSnapshot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import com.flowpowered.commons.map.TripleIntObjectMap;
import com.flowpowered.commons.map.impl.TTripleInt21ObjectHashMap;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.geo.snapshot.RegionSnapshot;
import com.flowpowered.engine.geo.region.FlowRegion;
import com.flowpowered.engine.geo.world.FlowWorld;

/**
 *
 */
public class FlowWorldSnapshot extends WorldSnapshot {
    private final TripleIntObjectMap<FlowRegionSnapshot> regions = new TTripleInt21ObjectHashMap<>();
    private final TObjectLongMap<FlowRegionSnapshot> lastUpdate = new TObjectLongHashMap<>();
    private long time;
    private long updateNumber = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public FlowWorldSnapshot(World world) {
        super(world.getUID(), world.getName());
        this.time = world.getAge();
    }

    @Override
    public boolean hasRegion(Vector3i position) {
        return hasRegion(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean hasRegion(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return regions.containsKey(x, y, z);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FlowRegionSnapshot getRegion(Vector3i position) {
        return getRegion(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public FlowRegionSnapshot getRegion(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return regions.get(x, y, z);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<Vector3i, RegionSnapshot> getRegions() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            final Map<Vector3i, RegionSnapshot> map = new HashMap<>(regions.size());
            for (FlowRegionSnapshot region : regions.valueCollection()) {
                map.put(region.getPosition(), region);
            }
            return map;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FlowChunkSnapshot getChunk(Vector3i position) {
        return getChunk(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public FlowChunkSnapshot getChunk(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            FlowRegionSnapshot get = regions.get(x >> Region.CHUNKS.BITS, y >> Region.CHUNKS.BITS, z >> Region.CHUNKS.BITS);
            if (get == null) {
                return null;
            }
            return get.getChunk(x, y, z);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getTime() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return time;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getUpdateNumber() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return updateNumber;
        } finally {
            lock.unlock();
        }
    }

    public void update(FlowWorld current) {
        if (!current.getUID().equals(id)) {
            throw new IllegalArgumentException("Cannot update from a world with another ID");
        }
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            final Set<Vector3i> validRegions = new HashSet<>();
            boolean changed = false;
            for (FlowRegion region : current.getFlowRegions()) {
                final Vector3i position = region.getPosition().toInt();
                FlowRegionSnapshot regionSnapshot = regions.get(position.getX(), position.getY(), position.getZ());
                if (regionSnapshot == null) {
                    regionSnapshot = region.getSnapshot();
                    regions.put(position.getX(), position.getY(), position.getZ(), regionSnapshot);
                    changed = true;
                }
                validRegions.add(position);
            }
            for (Iterator<FlowRegionSnapshot> iterator = regions.valueCollection().iterator(); iterator.hasNext(); ) {
                FlowRegionSnapshot next = iterator.next();
                final Vector3i position = next.getPosition();
                if (!validRegions.contains(position)) {
                    iterator.remove();
                    lastUpdate.remove(next);
                    changed = true;
                } else {
                    if (lastUpdate.get(next) < next.getUpdateNumber()) {
                        lastUpdate.put(next, next.getUpdateNumber());
                        changed = true;
                    }
                }
            }
            time = current.getAge();
            if (changed) {
                updateNumber++;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlowWorldSnapshot)) {
            return false;
        }
        final FlowWorldSnapshot snapshot = (FlowWorldSnapshot) o;
        return id.equals(snapshot.id);
    }

    @Override
    public int hashCode() {
        return 17 * id.hashCode();
    }
}
