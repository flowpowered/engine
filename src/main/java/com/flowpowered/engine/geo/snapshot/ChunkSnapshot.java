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
import com.flowpowered.engine.geo.chunk.SpoutChunk;

/**
 *
 */
public class ChunkSnapshot {
    private final short[] blockIDs = new short[Chunk.BLOCKS.VOLUME];
    private final short[] blockData = new short[Chunk.BLOCKS.VOLUME];
    private final WorldSnapshot world;
    private final RegionSnapshot region;
    private final Vector3i base;
    private long updateNumber = 0;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public ChunkSnapshot(WorldSnapshot world, RegionSnapshot region, Vector3i position) {
        this.world = world;
        this.region = region;
        this.base = position;
    }

    public WorldSnapshot getWorld() {
        return world;
    }

    public RegionSnapshot getRegion() {
        return region;
    }

    public Vector3i getPosition() {
        return base;
    }

    public int getX() {
        return base.getX();
    }

    public int getY() {
        return base.getY();
    }

    public int getZ() {
        return base.getZ();
    }

    public ChunkSnapshot getRelativeChunk(Vector3i relative) {
        return getRelativeChunk(relative.getX(), relative.getY(), relative.getZ());
    }

    public ChunkSnapshot getRelativeChunk(int x, int y, int z) {
        final Lock lock = this.lock.readLock();
        lock.lock();
        try {
            // We check to see if the chunk is in this chunk's region first, to avoid a map lookup for the other region
            final int regionX = getRegion().getBase().getX();
            final int regionY = getRegion().getBase().getY();
            final int regionZ = getRegion().getBase().getZ();
            final int otherChunkX = this.getX() + x;
            final int otherChunkY = this.getY() + y;
            final int otherChunkZ = this.getZ() + z;
            final int otherRegionX = otherChunkX / Region.CHUNKS.SIZE;
            final int otherRegionY = otherChunkY / Region.CHUNKS.SIZE;
            final int otherRegionZ = otherChunkZ / Region.CHUNKS.SIZE;
            if (regionX == otherRegionX && regionZ == otherRegionZ && regionY == otherRegionY) {
                // Get the chunk from the current region
                return getRegion().getChunk(otherChunkX, otherChunkY, otherChunkZ);
            }
            RegionSnapshot other = getWorld().getRegion(otherRegionX, otherRegionY, otherRegionZ);
            return other == null ? null : other.getChunk(otherChunkX, otherChunkY, otherChunkZ);
        } finally {
            lock.unlock();
        }
    }

    public BlockMaterial getMaterial(Vector3i position) {
        return getMaterial(position.getX(), position.getY(), position.getZ());
    }

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
    public boolean update(SpoutChunk current) {
        if (!current.getBase().toInt().equals(base) || !current.getWorld().getUID().equals(world.getID())) {
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
            ChunkSnapshot rel = getRelativeChunk(face.getIntOffset());
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
        if (!(o instanceof ChunkSnapshot)) {
            return false;
        }
        final ChunkSnapshot that = (ChunkSnapshot) o;
        if (!base.equals(that.base)) {
            return false;
        }
        return world.equals(that.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + base.hashCode();
        return result;
    }

    private static int getBlockIndex(Vector3i position) {
        return getBlockIndex(position.getX(), position.getY(), position.getZ());
    }

    private static int getBlockIndex(int x, int y, int z) {
        return (y & Chunk.BLOCKS.MASK) << Chunk.BLOCKS.DOUBLE_BITS | (z & Chunk.BLOCKS.MASK) << Chunk.BLOCKS.BITS | x & Chunk.BLOCKS.MASK;
    }
}
