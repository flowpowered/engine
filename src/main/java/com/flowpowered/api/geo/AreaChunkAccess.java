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
package com.flowpowered.api.geo;

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
     * @param save whether to save or not 
     */
    public void unloadChunk(int x, int y, int z, boolean save);

    /**
     * Gets the number of currently loaded chunks
     *
     * @return number of loaded chunks
     */
    public int getNumLoadedChunks();
}
