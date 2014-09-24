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

import java.util.Iterator;
import java.util.List;

import com.flowpowered.api.Engine;
import com.flowpowered.commons.BitSize;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.player.Player;
import com.flowpowered.api.geo.AreaChunkAccess;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.LocalAreaAccess;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.util.UnloadSavable;

/**
 * Represents a cube containing 16x16x16 Chunks (256x256x256 Blocks)
 */
public abstract class Region extends Cube implements AreaChunkAccess, LocalAreaAccess, Iterable<Chunk>, UnloadSavable {
    /**
     * Stores the size of the amount of chunks in this Region
     */
    public static final BitSize CHUNKS = new BitSize(4);
    /**
     * Stores the size of the amount of blocks in this Region
     */
    public static final BitSize BLOCKS = new BitSize(CHUNKS.BITS + Chunk.BLOCKS.BITS);

    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final int chunkX;
    private final int chunkY;
    private final int chunkZ;
    private final Engine engine;

    public Region(World world, int x, int y, int z) {
        super(new Point(world, x, y, z), BLOCKS.SIZE);
        this.blockX = getRegionX() << BLOCKS.BITS;
        this.blockY = getRegionY() << BLOCKS.BITS;
        this.blockZ = getRegionZ() << BLOCKS.BITS;
        this.chunkX = getRegionX() << CHUNKS.BITS;
        this.chunkY = getRegionY() << CHUNKS.BITS;
        this.chunkZ = getRegionZ() << CHUNKS.BITS;
        this.engine = world.getEngine();
    }

    /**
     * Gets the x-coordinate of this region as a Block coordinate
     *
     * @return the x-coordinate of the first block in this region
     */
    public int getBlockX() {
        return this.blockX;
    }

    /**
     * Gets the y-coordinate of this region as a Block coordinate
     *
     * @return the y-coordinate of the first block in this region
     */
    public int getBlockY() {
        return this.blockY;
    }

    /**
     * Gets the z-coordinate of this region as a Block coordinate
     *
     * @return the z-coordinate of the first block in this region
     */
    public int getBlockZ() {
        return this.blockZ;
    }

    /**
     * Gets the x-coordinate of this region as a Chunk coordinate
     *
     * @return the x-coordinate of the first chunk in this region
     */
    public int getChunkX() {
        return this.chunkX;
    }

    /**
     * Gets the y-coordinate of this region as a Chunk coordinate
     *
     * @return the y-coordinate of the first chunk in this region
     */
    public int getChunkY() {
        return this.chunkY;
    }

    /**
     * Gets the z-coordinate of this region as a Chunk coordinate
     *
     * @return the z-coordinate of the first chunk in this region
     */
    public int getChunkZ() {
        return this.chunkZ;
    }

    public final int getRegionX() {
        return (int) super.getX();
    }

    public final int getRegionY() {
        return (int) super.getY();
    }

    public final int getRegionZ() {
        return (int) super.getZ();
    }

    @Override
    public boolean containsBlock(int x, int y, int z) {
        return x >> BLOCKS.BITS == getRegionX() && y >> BLOCKS.BITS == getRegionY() && z >> BLOCKS.BITS == getRegionZ();
    }

    @Override
    public boolean containsChunk(int x, int y, int z) {
        return x >> CHUNKS.BITS == getRegionX() && y >> CHUNKS.BITS == getRegionY() && z >> CHUNKS.BITS == getRegionZ();
    }

    /**
     * Gets all entities with the specified type.
     *
     * @return A set of entities with the specified type.
     */
    public abstract List<Entity> getAll();

    /**
     * Gets an entity by its id.
     *
     * @param id The id.
     * @return The entity, or {@code null} if it could not be found.
     */
    public abstract Entity getEntity(int id);

    public abstract List<Player> getPlayers();

    public Engine getEngine() {
        return engine;
    }

    @Override
    public Iterator<Chunk> iterator() {
        return new ChunkIterator();
    }

    private class ChunkIterator implements Iterator<Chunk> {
        private Chunk next;

        public ChunkIterator() {
            loop:
            for (int dx = 0; dx < CHUNKS.SIZE; dx++) {
                for (int dy = 0; dy < CHUNKS.SIZE; dy++) {
                    for (int dz = 0; dz < CHUNKS.SIZE; dz++) {
                        next = getChunk(dx, dy, dz, LoadOption.NO_LOAD);
                        if (next != null) {
                            break loop;
                        }
                    }
                }
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Chunk next() {
            Chunk current = next;
            next = null;
            final int cx = current.getChunkX() & CHUNKS.MASK;
            final int cy = current.getChunkY() & CHUNKS.MASK;
            final int cz = current.getChunkZ() & CHUNKS.MASK;
            for (int dx = cx; dx < CHUNKS.SIZE; dx++) {
                for (int dy = cy; dy < CHUNKS.SIZE; dy++) {
                    for (int dz = cz; dz < CHUNKS.SIZE; dz++) {
                        next = getChunk(dx, dy, dz, LoadOption.NO_LOAD);
                        if (next != null && next != current) {
                            return current;
                        }
                    }
                }
            }
            return current;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Operation not supported");
        }
    }
}
