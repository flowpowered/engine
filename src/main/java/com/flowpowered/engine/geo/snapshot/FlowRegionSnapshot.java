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

import com.flowpowered.api.geo.snapshot.RegionSnapshot;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.geo.region.FlowRegion;
import com.flowpowered.math.vector.Vector3i;

/**
 *
 */
public class FlowRegionSnapshot extends RegionSnapshot {
    private final FlowChunkSnapshot[] chunks = new FlowChunkSnapshot[Region.CHUNKS.VOLUME];
    private long updateNumber = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public FlowRegionSnapshot(FlowWorldSnapshot world, Vector3i position) {
        super(position, world);
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
            return chunks[FlowRegion.getChunkKey(x, y, z)];
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FlowChunkSnapshot[] getChunks() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return Arrays.copyOf(chunks, chunks.length);
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

    public boolean update(FlowRegion current) {
        if (!current.getPosition().toInt().equals(position)) {
            throw new IllegalArgumentException("Cannot update from a region with another ID");
        }
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            boolean changed = false;
            FlowChunk[] currentChunks = current.getChunks();
            for (int i = 0; i < Region.CHUNKS.VOLUME; i++) {
                FlowChunk currentChunk = currentChunks[i];
                FlowChunkSnapshot currentSnapshot = chunks[i];
                if (currentChunk == null && currentSnapshot == null) {
                    continue;
                } else if (currentChunk != null && currentSnapshot == null) {
                    FlowChunkSnapshot chunkSnapshot = new FlowChunkSnapshot((FlowWorldSnapshot) world, this, currentChunk.getPosition().toInt());
                    chunkSnapshot.update(currentChunk);
                    chunks[i] = chunkSnapshot;
                    changed = true;
                } else if (currentChunk == null && currentSnapshot != null) {
                    chunks[i] = null;
                    changed = true;
                } else if (currentChunk != null && currentSnapshot != null) {
                    if (currentSnapshot.update(currentChunk)) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                updateNumber++;
            }
            return changed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlowRegionSnapshot)) {
            return false;
        }
        final FlowRegionSnapshot snapshot = (FlowRegionSnapshot) o;
        return position.equals(snapshot.position);
    }

    @Override
    public int hashCode() {
        return 17 * position.hashCode();
    }
}
