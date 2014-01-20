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

import com.flowpowered.commons.datatable.SerializableMap;
import com.flowpowered.api.component.block.BlockComponent;
import com.flowpowered.api.entity.EntitySnapshot;
import com.flowpowered.api.geo.AreaBlockSource;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.material.BlockMaterial;

public abstract class ChunkSnapshot extends Cube implements AreaBlockSource {
	/**
	 * Internal size of a side of a chunk
	 */
	public final static int CHUNK_SIZE = 16;
	/**
	 * Number of bits on the side of a chunk
	 */
	public final static int CHUNK_SIZE_BITS = 4;
	/**
	 * Mask to convert a block integer coordinate into the chunk's base
	 */
	public final static int BASE_MASK = -CHUNK_SIZE;

	public ChunkSnapshot(World world, float x, float y, float z) {
		super(new Point(world, x, y, z), CHUNK_SIZE);
	}

	/**
	 * Gets a copy of the raw block ids.
	 *
	 * @return raw block ids
	 */
	public abstract short[] getBlockIds();

	/**
	 * Gets a copy of the raw block data.
	 *
	 * @return block data
	 */
	public abstract short[] getBlockData();

	/**
	 * Gets the region that this chunk is located in.
	 */
	public abstract Region getRegion();

	/**
	 * Gets an unmodifiable list of all of the entity snapshots associated with this chunk.
	 *
	 * @return the entities
	 */
	public abstract List<EntitySnapshot> getEntities();

	/**
	 * Gets if this chunk snapshot had already been populated.
	 *
	 * @return if the chunk snapshot was populated.
	 */
	public abstract boolean isPopulated();

	/**
	 * A thread-safe copy of the map of data attached to the chunk.
	 *
	 * @return data map
	 */
	public abstract SerializableMap getDataMap();

	/**
	 * Gets a unmodifiable list of all of the block component snapshots associated with this chunk.
	 *
	 * @return list of block component snapshots
	 */
	public abstract List<BlockComponentSnapshot> getBlockComponents();

	public static enum SnapshotType {
		/**
		 * Loads no block data (ids, data, skylight, blocklight) in the snapshot
		 */
		NO_BLOCK_DATA,
		/**
		 * Loads only block ids, no block data, skylight, or blocklight
		 */
		BLOCK_IDS_ONLY,
		/**
		 * Loads only block ids and block data, no skylight or blocklight
		 */
		BLOCKS_ONLY,
		/**
		 * Loads only skylight and blocklight, no block ids or block data
		 */
		LIGHT_ONLY,
		/**
		 * Loads block ids, block data, skylight, and blocklight
		 */
		BOTH,
	}

	public static enum EntityType {
		/**
		 * Saves no entity information in the snapshot
		 */
		NO_ENTITIES,
		/**
		 * Saves class and datatable information regarding block components in the snapshot
		 */
		BLOCK_COMPONENTS,
		/**
		 * Saves entity snapshot information
		 */
		ENTITIES,
		/**
		 * Saves both entity and block component information in the snapshot
		 */
		BOTH
	}

	public static enum ExtraData {
		/**
		 * Loads no extra data in the snapshot
		 */
		NO_EXTRA_DATA,
		/**
		 * Loads only extra biome data in the snapshot
		 */
		BIOME_DATA,
		/**
		 * Loads only extra datatable information in the snapshot
		 */
		DATATABLE,
		/**
		 * Loads both biome and datatable information in the snapshot
		 */
		BOTH;
	}

	public static interface BlockComponentSnapshot {
		/**
		 * Gets the world block x-coordinate for this snapshot
		 *
		 * @return x-coordinate
		 */
		public int getX();

		/**
		 * Gets the world block x-coordinate for this snapshot
		 *
		 * @return y-coordinate
		 */
		public int getY();

		/**
		 * Gets the world block x-coordinate for this snapshot
		 *
		 * @return z-coordinate
		 */
		public int getZ();

		/**
		 * Gets the block component class
		 *
		 * @return block component class
		 */
		public Set<Class<? extends BlockComponent>> getComponents();

		/**
		 * Gets a copy of the block component data
		 *
		 * @return data
		 */
		public SerializableMap getData();
	}
}
