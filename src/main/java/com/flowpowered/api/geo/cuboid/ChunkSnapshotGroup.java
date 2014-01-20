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
package com.flowpowered.api.geo.cuboid;

import com.flowpowered.api.material.BlockMaterial;

public interface ChunkSnapshotGroup {
	public int getX();

	public int getY();

	public int getZ();

	/**
	 * Gets if the chunk was unloaded.  Unload models only indicate an unload occurred and contain no data.
	 */
	public boolean isUnload();

	/**
	 * Gets the current center chunk of this model
	 */
	public ChunkSnapshot getCenter();

	/**
	 * Clears all references to live chunks and regions
	 */
	public void cleanUp();

	/**
	 * Gets the chunk at world chunk coordinates<br> Note: Coordinates must be within this model, or index out of bounds will be thrown.
	 *
	 * @param cx coordinate of the chunk
	 * @param cy coordinate of the chunk
	 * @param cz coordinate of the chunk
	 * @return The chunk, or null if not available
	 */
	public ChunkSnapshot getChunk(int cx, int cy, int cz);

	/**
	 * Gets the chunk at world block coordinates<br> Note: Coordinates must be within this model, or index out of bounds will be thrown.
	 *
	 * @param bx coordinate of the block
	 * @param by coordinate of the block
	 * @param bz coordinate of the block
	 * @return The chunk, or null if not available
	 */
	public ChunkSnapshot getChunkFromBlock(int bx, int by, int bz);

	/**
	 * Gets the block material at the chunk block coordinates.<br> Note: Coordinates must be within this model, or index out of bounds will be thrown.
	 *
	 * @param bx coordinate of the block
	 * @param by coordinate of the block
	 * @param bz coordinate of the block
	 * @return The block, or null if not available
	 */
	public BlockMaterial getBlock(int bx, int by, int bz);
}
