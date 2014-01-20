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
package com.flowpowered.engine.geo.chunk;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.ChunkSnapshot;
import com.flowpowered.api.geo.cuboid.ChunkSnapshotGroup;
import com.flowpowered.api.material.BlockMaterial;

public class FlowChunkSnapshotGroup implements ChunkSnapshotGroup {
	private final int cx, cy, cz;
	private final ChunkSnapshot[][][] chunks;

    public FlowChunkSnapshotGroup(int cx, int cy, int cz, ChunkSnapshot[][][] chunks) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.chunks = chunks;
    }

    @Override
    public int getX() {
        return cx;
    }

    @Override
    public int getY() {
        return cy;
    }

    @Override
    public int getZ() {
        return cz;
    }

    @Override
    public boolean isUnload() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChunkSnapshot getCenter() {
        return chunks[1][1][1];
    }

    @Override
    public void cleanUp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	/**
	 * Gets the chunk at world chunk coordinates<br> Note: Coordinates must be within this model, or index out of bounds will be thrown.
	 *
	 * @param worldX coordinate of the chunk
	 * @param worldY coordinate of the chunk
	 * @param worldZ coordinate of the chunk
	 * @return The chunk, or null if not available
	 */
	@Override
	public ChunkSnapshot getChunk(int worldX, int worldY, int worldZ) {
		return chunks[worldX - this.cx + 1][worldY - this.cy + 1][worldZ - this.cz + 1];
	}

    @Override
    public ChunkSnapshot getChunkFromBlock(int bx, int by, int bz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BlockMaterial getBlock(int localX, int localY, int localZ) {
        ChunkSnapshot chunk = getChunk(localX >> Chunk.BLOCKS.BITS, localY >> Chunk.BLOCKS.BITS, localZ >> Chunk.BLOCKS.BITS);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockMaterial(localX, localY, localZ);
    }
}
