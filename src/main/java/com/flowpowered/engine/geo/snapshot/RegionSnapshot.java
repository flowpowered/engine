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
package com.flowpowered.engine.geo.snapshot;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.engine.geo.chunk.SpoutChunk;
import com.flowpowered.engine.geo.region.SpoutRegion;
import com.flowpowered.math.vector.Vector3i;

/**
 *
 */
public class RegionSnapshot {
    private final ChunkSnapshot[] chunks = new ChunkSnapshot[Region.CHUNKS.VOLUME];
    private final WorldSnapshot world;
    private final Vector3i base;
    private long updateNumber = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public RegionSnapshot(WorldSnapshot snapshot, Vector3i base) {
        this.world = snapshot;
        this.base = base;
    }

    public Vector3i getBase() {
        return base;
    }

    public ChunkSnapshot getChunk(Vector3i position) {
        return getChunk(position.getX(), position.getY(), position.getZ());
    }

    public ChunkSnapshot getChunk(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return chunks[SpoutRegion.getChunkKey(x, y, z)];
        } finally {
            lock.unlock();
        }
    }

    public ChunkSnapshot[] getChunks() {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            return Arrays.copyOf(chunks, chunks.length);
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

    public boolean update(SpoutRegion current) {
        if (!current.getBase().toInt().equals(base)) {
            throw new IllegalArgumentException("Cannot update from a region with another ID");
        }
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            boolean changed = false;
            SpoutChunk[] currentChunks = current.getChunks();
            for (int i = 0; i < Region.CHUNKS.VOLUME; i++) {
                SpoutChunk currentChunk = currentChunks[i];
                ChunkSnapshot currentSnapshot = chunks[i];
                if (currentChunk == null && currentSnapshot == null) {
                    continue;
                } else if (currentChunk != null && currentSnapshot == null) {
                    ChunkSnapshot chunkSnapshot = new ChunkSnapshot(world, this, currentChunk.getBase().toInt());
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
        if (!(o instanceof RegionSnapshot)) {
            return false;
        }
        final RegionSnapshot snapshot = (RegionSnapshot) o;
        return base.equals(snapshot.base);
    }

    @Override
    public int hashCode() {
        return 17 * base.hashCode();
    }
}
