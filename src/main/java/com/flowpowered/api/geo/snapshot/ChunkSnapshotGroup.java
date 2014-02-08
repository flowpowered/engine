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
package com.flowpowered.api.geo.snapshot;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.math.vector.Vector3i;

/**
 * A chunk and it's immediate neighbours (BTNESW), used for meshing the chunk including it's edge blocks with proper occlusion.
 */
public class ChunkSnapshotGroup {
    private final ChunkSnapshot middle;
    private final ChunkSnapshot top;
    private final ChunkSnapshot bottom;
    private final ChunkSnapshot north;
    private final ChunkSnapshot east;
    private final ChunkSnapshot south;
    private final ChunkSnapshot west;

    /**
     * Constructs a new snapshot group from the middle chunk snapshot and the world snapshot. The world snapshot from the chunk will be used to source the neighbouring chunks (if they exist).
     *
     * @param middle The middle chunk
     */
    public ChunkSnapshotGroup(ChunkSnapshot middle) {
        this.middle = middle;
        this.top = middle.getRelativeChunk(Vector3i.UP);
        this.bottom = middle.getRelativeChunk(Vector3i.UP.mul(-1));
        this.north = middle.getRelativeChunk(Vector3i.RIGHT.mul(-1));
        this.south = middle.getRelativeChunk(Vector3i.RIGHT);
        this.east = middle.getRelativeChunk(Vector3i.FORWARD.mul(-1));
        this.west = middle.getRelativeChunk(Vector3i.FORWARD);
    }

    /**
     * Returns the material at the position, looking at the directly neighbouring chunks if the position is outside the chunk.
     *
     * @param position The position to lookup the material at
     * @return The material or null if missing
     */
    public BlockMaterial getMaterial(Vector3i position) {
        return getMaterial(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Returns the material at the position, looking at the directly neighbouring chunks if the position is outside the chunk.
     *
     * @param x The x coordinate of the position
     * @param y The y coordinate of the position
     * @param z The z coordinate of the position
     * @return The material or null if missing
     */
    public BlockMaterial getMaterial(int x, int y, int z) {
        if (x < 0) {
            return north != null ? north.getMaterial(x, y, z) : null;
        } else if (x >= Chunk.BLOCKS.SIZE) {
            return south != null ? south.getMaterial(x, y, z) : null;
        } else if (y < 0) {
            return bottom != null ? bottom.getMaterial(x, y, z) : null;
        } else if (y >= Chunk.BLOCKS.SIZE) {
            return top != null ? top.getMaterial(x, y, z) : null;
        } else if (z < 0) {
            return east != null ? east.getMaterial(x, y, z) : null;
        } else if (z >= Chunk.BLOCKS.SIZE) {
            return west != null ? west.getMaterial(x, y, z) : null;
        }
        return middle.getMaterial(x, y, z);
    }

    public ChunkSnapshot getMiddle() {
        return middle;
    }
}
