/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package com.flowpowered.api.geo;

import java.util.List;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.math.vector.Vector3f;

public interface AreaChunkAccess extends AreaBlockAccess {
	/**
	 * Gets if a chunk is contained in this area
	 *
	 * @param x coordinate of the chunk
	 * @param y coordinate of the chunk
	 * @param z coordinate of the chunk
	 * @return True if it is contained, False if not
	 */
	public boolean containsChunk(int x, int y, int z);

	/**
	 * Gets the {@link Chunk} at chunk coordinates (x, y, z)
	 *
	 * @param x coordinate of the chunk
	 * @param y coordinate of the chunk
	 * @param z coordinate of the chunk
	 * @param loadopt to control whether to load and/or generate the chunk, if needed
	 * @return the chunk
	 */
	public Chunk getChunk(int x, int y, int z, LoadOption loadopt);

	/**
	 * Gets the {@link Chunk} at block coordinates (x, y, z)
	 *
	 * @param x coordinate of the block
	 * @param y coordinate of the block
	 * @param z coordinate of the block
	 * @param loadopt to control whether to load and/or generate the chunk, if needed
	 * @return the chunk
	 */
	public Chunk getChunkFromBlock(int x, int y, int z, LoadOption loadopt);

	/**
	 * Gets the {@link Chunk} at the given position
	 *
	 * @param position of the block
	 * @param loadopt to control whether to load and/or generate the chunk, if needed
	 * @return the chunk
	 */
	public Chunk getChunkFromBlock(Vector3f position, LoadOption loadopt);

	/**
	 * Queues a chunk for saving at the next available opportunity.
	 *
	 * @param x coordinate of the chunk
	 * @param y coordinate of the chunk
	 * @param z coordinate of the chunk
	 */
	public void saveChunk(int x, int y, int z);

	/**
	 * Unloads a chunk, and queues it for saving, if requested.
	 *
	 * @param x coordinate of the chunk
	 * @param y coordinate of the chunk
	 * @param z coordinate of the chunk
	 */
	public void unloadChunk(int x, int y, int z, boolean save);

	/**
	 * Gets the number of currently loaded chunks
	 *
	 * @return number of loaded chunks
	 */
	public int getNumLoadedChunks();
}
