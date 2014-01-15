/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spoutcraft <http://spoutcraft.org/>
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
package org.spout.engine.geo.snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.flowpowered.commons.map.TripleIntObjectMap;
import com.flowpowered.commons.map.impl.TTripleInt21ObjectHashMap;
import com.flowpowered.math.vector.Vector3i;

import org.spout.api.geo.World;
import org.spout.engine.geo.region.SpoutRegion;
import org.spout.engine.geo.world.SpoutWorld;

/**
 *
 */
public class WorldSnapshot {
    private final TripleIntObjectMap<RegionSnapshot> regions = new TTripleInt21ObjectHashMap<>();
    private final UUID id;
    private final String name;
    private long time;
    private long updateNumber = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public WorldSnapshot(World world) {
        this.id = world.getUID();
        this.name = world.getName();
        this.time = world.getAge();
    }

    public UUID getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean hasRegion(Vector3i position) {
        return hasRegion(position.getX(), position.getY(), position.getZ());
    }

    public boolean hasRegion(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return regions.containsKey(x, y, z);
        } finally {
            lock.unlock();
        }
    }

    public RegionSnapshot getRegion(Vector3i position) {
        return getRegion(position.getX(), position.getY(), position.getZ());
    }

    public RegionSnapshot getRegion(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return regions.get(x, y, z);
        } finally {
            lock.unlock();
        }
    }

    public Map<Vector3i, RegionSnapshot> getRegions() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            final Map<Vector3i, RegionSnapshot> map = new HashMap<>(regions.size());
            for (RegionSnapshot region : regions.valueCollection()) {
                map.put(region.getBase(), region);
            }
            return map;
        } finally {
            lock.unlock();
        }
    }

    public RegionSnapshot getChunk(Vector3i position) {
        return getRegion(position.getX(), position.getY(), position.getZ());
    }

    public RegionSnapshot getChunk(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return regions.get(x, y, z);
        } finally {
            lock.unlock();
        }
    }

    public long getTime() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return time;
        } finally {
            lock.unlock();
        }
    }

    public long getUpdateNumber() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return updateNumber;
        } finally {
            lock.unlock();
        }
    }

    public void update(SpoutWorld current) {
        if (!current.getUID().equals(id)) {
            throw new IllegalArgumentException("Cannot update from a world with another ID");
        }
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            final Set<Vector3i> validRegions = new HashSet<>();
            boolean changed = false;
            for (SpoutRegion region : current.getSpoutRegions()) {
                final Vector3i base = region.getBase().toInt();
                RegionSnapshot regionSnapshot = regions.get(base.getX(), base.getY(), base.getZ());
                if (regionSnapshot == null) {
                    regionSnapshot = region.getSnapshot();
                    regions.put(base.getX(), base.getY(), base.getZ(), regionSnapshot);
                    changed = true;
                }
                validRegions.add(base);
            }
            for (Iterator<RegionSnapshot> iterator = regions.valueCollection().iterator(); iterator.hasNext(); ) {
                final Vector3i position = iterator.next().getBase();
                if (!validRegions.contains(position)) {
                    iterator.remove();
                    changed = true;
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
        if (!(o instanceof WorldSnapshot)) {
            return false;
        }
        final WorldSnapshot snapshot = (WorldSnapshot) o;
        return id.equals(snapshot.id);
    }

    @Override
    public int hashCode() {
        return 17 * id.hashCode();
    }
}
