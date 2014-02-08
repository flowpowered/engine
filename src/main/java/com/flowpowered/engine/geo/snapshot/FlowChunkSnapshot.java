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

import com.flowpowered.api.geo.snapshot.ChunkSnapshot;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.flowpowered.commons.store.block.AtomicBlockStore;
import com.flowpowered.math.vector.Vector3i;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.material.block.BlockFace;
import com.flowpowered.api.material.block.BlockFaces;
import com.flowpowered.engine.geo.chunk.FlowChunk;

/**
 *
 */
public class FlowChunkSnapshot extends ChunkSnapshot {
    private final short[] blockIDs = new short[Chunk.BLOCKS.VOLUME];
    private final short[] blockData = new short[Chunk.BLOCKS.VOLUME];
    private long updateNumber = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public FlowChunkSnapshot(FlowWorldSnapshot world, FlowRegionSnapshot region, Vector3i position) {
        super(position, region, world);
    }

    @Override
    public FlowChunkSnapshot getRelativeChunk(Vector3i relative) {
        return getRelativeChunk(relative.getX(), relative.getY(), relative.getZ());
    }

    @Override
    public FlowChunkSnapshot getRelativeChunk(int x, int y, int z) {
        // We check to see if the chunk is in this chunk's region first, to avoid a map lookup for the other region
        final int regionX = getRegion().getPosition().getX();
        final int regionY = getRegion().getPosition().getY();
        final int regionZ = getRegion().getPosition().getZ();
        final int otherChunkX = this.getX() + x;
        final int otherChunkY = this.getY() + y;
        final int otherChunkZ = this.getZ() + z;
        final int otherRegionX = otherChunkX / Region.CHUNKS.SIZE;
        final int otherRegionY = otherChunkY / Region.CHUNKS.SIZE;
        final int otherRegionZ = otherChunkZ / Region.CHUNKS.SIZE;
        if (regionX == otherRegionX && regionZ == otherRegionZ && regionY == otherRegionY) {
            // Get the chunk from the current region
            return (FlowChunkSnapshot) getRegion().getChunk(otherChunkX, otherChunkY, otherChunkZ);
        }
        FlowRegionSnapshot other = (FlowRegionSnapshot) getWorld().getRegion(otherRegionX, otherRegionY, otherRegionZ);
        return other == null ? null : other.getChunk(otherChunkX, otherChunkY, otherChunkZ);
    }

    @Override
    public BlockMaterial getMaterial(Vector3i position) {
        return getMaterial(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public BlockMaterial getMaterial(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            final int index = getBlockIndex(x, y, z);
            return BlockMaterial.get(blockIDs[index], blockData[index]);
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

    /**
     * Updates the snapshot to the current chunk passed to the constructor. The chunk passed must be a the same location and world than the snapshot. Returns whether or not the snapshot state has
     * changed. Clears the chunk block store dirty arrays.
     *
     * @param current The current chunk to update from
     * @return Whether or not the snapshot state has changed
     */
    public boolean update(FlowChunk current) {
        if (!current.getPosition().toInt().equals(position) || !current.getWorld().getUID().equals(world.getID())) {
            throw new IllegalArgumentException("Cannot accept a chunk from another position or world");
        }
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            // TODO: update only the dirty blocks, unless the dirty arrays are overflown
            final AtomicBlockStore blocks = current.getBlockStore();
            if (blocks.isDirty()) {
                blocks.getBlockIdArray(blockIDs);
                blocks.getDataArray(blockData);
                blocks.resetDirtyArrays();
                updateNumber++;
                //touchNeighbors();
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void touch() {
        final Lock lock = this.lock.writeLock();
        lock.lock();
        try {
            updateNumber++;
        } finally {
            lock.unlock();
        }
    }

    private void touchNeighbors() {
        for (BlockFace face : BlockFaces.NESWBT) {
            FlowChunkSnapshot rel = getRelativeChunk(face.getOffset());
            if (rel != null) {
                rel.touch();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlowChunkSnapshot)) {
            return false;
        }
        final FlowChunkSnapshot that = (FlowChunkSnapshot) o;
        if (!position.equals(that.position)) {
            return false;
        }
        return world.equals(that.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + position.hashCode();
        return result;
    }

    private static int getBlockIndex(Vector3i position) {
        return getBlockIndex(position.getX(), position.getY(), position.getZ());
    }

    private static int getBlockIndex(int x, int y, int z) {
        return (y & Chunk.BLOCKS.MASK) << Chunk.BLOCKS.DOUBLE_BITS | (z & Chunk.BLOCKS.MASK) << Chunk.BLOCKS.BITS | x & Chunk.BLOCKS.MASK;
    }
}
