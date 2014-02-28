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

import java.util.List;
import java.util.Set;

import com.flowpowered.api.Engine;
import com.flowpowered.commons.BitSize;

import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.player.Player;
import com.flowpowered.api.geo.AreaBlockAccess;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldManager;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.reference.WorldReference;
import com.flowpowered.api.material.block.BlockFace;
import com.flowpowered.api.util.UnloadSavable;
import com.flowpowered.math.vector.Vector3i;

/**
 * Represents a cube containing 16x16x16 Blocks
 */
public abstract class Chunk extends Cube implements AreaBlockAccess, UnloadSavable {
    /**
     * Stores the size of the amount of blocks in this Chunk
     */
    public static final BitSize BLOCKS = new BitSize(4);
	/**
	 * Mask to convert a block integer coordinate into the point base
	 */
	public final static int POINT_BASE_MASK = -BLOCKS.SIZE;
    /**
     * Mask to convert a block integer coordinate into the point base
     */
    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final Engine engine;

    public Chunk(Engine engine, WorldReference world, int x, int y, int z) {
        super(new Point(world, x, y, z), BLOCKS.SIZE);
        this.blockX = getChunkX() << BLOCKS.BITS;
        this.blockY = getChunkY() << BLOCKS.BITS;
        this.blockZ = getChunkZ() << BLOCKS.BITS;
        this.engine = engine;
    }

    /**
     * Fills the given block container with the block data for this chunk
     */
    public abstract void fillBlockContainer(BlockContainer container);

    /**
     * Gets the region that this chunk is located in
     *
     * @return region
     */
    public abstract Region getRegion();

    /**
     * Populates the chunk with all the Populators attached to the WorldGenerator of its world.
     */
    public abstract boolean populate();

    /**
     * Populates the chunk with all the Populators attached to the WorldGenerator of its world.
     *
     * @param force forces to populate the chunk even if it already has been populated.
     */
    public abstract boolean populate(boolean force);

    /**
     * Populates the chunk with all the Populators attached to the WorldGenerator of its world.<br> <br> Warning: populating with force observer should not be called from within populators as it could
     * lead to a population cascade
     *
     * @param sync queues the population to occur at a later time
     * @param observe forces the chunk to be observed for population
     */
    public abstract void populate(boolean sync, boolean observe);

    /**
     * Populates the chunk with all the Populators attached to the WorldGenerator of its world.<br> <br> Warning: populating with force observer should not be called from within populators as it could
     * lead to a population cascade
     *
     * @param sync queues the population to occur at a later time
     * @param observe forces the chunk to be observed for population
     * @param priority adds the chunk to the high priority queue
     */
    public abstract void populate(boolean sync, boolean observe, boolean priority);

    /**
     * Gets if this chunk already has been populated.
     *
     * @return if the chunk is populated.
     */
    public abstract boolean isPopulated();

    /**
     * Gets the entities in the chunk at the last snapshot
     *
     * @return the entities
     */
    public abstract List<Entity> getEntities();

    /**
     * Gets the entities currently in the chunk
     *
     * @return the entities
     */
    public abstract List<Entity> getLiveEntities();

    /**
     * Gets the number of observers viewing this chunk. If the number of observing entities falls to zero, this chunk may be reaped at any time.
     *
     * @return number of observers
     */
    public abstract int getNumObservers();

    /**
     * Gets the observing players of this chunk (done based on the player's view distance).
     *
     * @return List containing the observing players
     */
    public abstract Set<? extends Player> getObservingPlayers();

    /**
     * Gets the observers of this chunk
     *
     * @return Set containing the observing players
     */
    public abstract Set<? extends Entity> getObservers();

    /**
     * Refresh the distance between a player and the chunk, and adds the player as an observer if not previously observing.
     *
     * @param player the player
     * @return false if the player was already observing the chunk
     */
    public abstract boolean refreshObserver(Entity player);

    /**
     * De-register a player as observing the chunk.
     *
     * @param player the player
     * @return true if the player was observing the chunk
     */
    public abstract boolean removeObserver(Entity player);

    @Override
    public boolean containsBlock(int x, int y, int z) {
        return x >> BLOCKS.BITS == getChunkX() && y >> BLOCKS.BITS == getChunkY() && z >> BLOCKS.BITS == getChunkZ();
    }

    /**
     * Gets the x-coordinate of this chunk as a Block coordinate
     *
     * @return the x-coordinate of the first block in this chunk
     */
    public int getBlockX() {
        return blockX;
    }

    /**
     * Gets the y-coordinate of this chunk as a Block coordinate
     *
     * @return the y-coordinate of the first block in this chunk
     */
    public int getBlockY() {
        return blockY;
    }

    /**
     * Gets the z-coordinate of this chunk as a Block coordinate
     *
     * @return the z-coordinate of the first block in this chunk
     */
    public int getBlockZ() {
        return blockZ;
    }

    public int getChunkX() {
        return (int) getX();
    }

    public int getChunkY() {
        return (int) getY();
    }

    public int getChunkZ() {
        return (int) getZ();
    }

    /**
     * Gets a chunk relative to this chunk
     *
     * @param x
     * @param y
     * @param z
     * @param opt
     * @return The Chunk, or null if not loaded and load is False
     */
    public Chunk getRelative(int x, int y, int z, LoadOption opt) {
        // We check to see if the chunk is in this chunk's region first, to avoid a map lookup for the other region
        final int otherChunkX = this.getChunkX() + x;
        final int otherChunkY = this.getChunkY() + y;
        final int otherChunkZ = this.getChunkZ() + z;
        final int regionX = getRegion().getRegionX();
        final int regionY = getRegion().getRegionY();
        final int regionZ = getRegion().getRegionZ();
        final int otherRegionX = otherChunkX >> Region.CHUNKS.BITS;
        final int otherRegionY = otherChunkY >> Region.CHUNKS.BITS;
        final int otherRegionZ = otherChunkZ >> Region.CHUNKS.BITS;
        if (regionX == otherRegionX && regionZ == otherRegionZ && regionY == otherRegionY) {
            // Get the chunk from the current region
            return getRegion().getChunk(otherChunkX - otherRegionX, otherChunkY - otherRegionY, otherChunkZ - otherRegionZ, opt);
        }
        return this.getWorld().refresh(engine.getWorldManager()).getChunk(otherChunkX, otherChunkY, otherChunkZ, opt);
    }

    /**
     * Gets a chunk relative to this chunk
     *
     * @param offset of the chunk relative to this chunk
     * @param opt True to load the chunk if it is not yet loaded
     * @return The Chunk, or null if not loaded and load is False
     */
    public Chunk getRelative(Vector3i offset, LoadOption opt) {
        return this.getWorld().refresh(engine.getWorldManager()).getChunk(this.getChunkX() + offset.getX(), this.getChunkY() + offset.getY(), this.getChunkZ() + offset.getZ(), opt);
    }

    /**
     * Gets a chunk relative to this chunk
     *
     * @param offset of the chunk relative to this chunk
     * @param opt True to load the chunk if it is not yet loaded
     * @return The Chunk, or null if not loaded and load is False
     */
    public Chunk getRelative(BlockFace offset, LoadOption opt) {
        return this.getRelative(offset.getOffset(), opt);
    }

    /**
     * Gets the generation index for this chunk.  Only chunks generated as part of the same bulk initialize have the same index.
     *
     * @return a unique generation id, or -1 if the chunk was loaded from disk
     */
    public abstract int getGenerationIndex();

	/**
	 * Converts a point in such a way that it points to the first block (the base block) of the chunk<br> This is similar to performing the following operation on the x, y and z coordinate:<br> - Convert
	 * to the chunk coordinate<br> - Multiply by chunk size
	 */
	public static Point pointToBase(Point p) {
		return new Point(p.getWorld(), p.getBlockX() & POINT_BASE_MASK, p.getBlockY() & POINT_BASE_MASK, p.getBlockZ() & POINT_BASE_MASK);
	}

    public Engine getEngine() {
        return engine;
    }
}
