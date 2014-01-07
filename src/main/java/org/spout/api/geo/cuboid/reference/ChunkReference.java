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
package org.spout.api.geo.cuboid.reference;

import java.lang.ref.WeakReference;

import org.spout.api.geo.LoadOption;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.geo.discrete.Point;

/**
 * This holds a {@code WeakReference<Chunk>} that can be used streamline the get() with a isLoaded check. It also adds a
 * store of a {@code Point} representing the base. Because of this, a ChunkReference may contain only base info.
 */
public class ChunkReference {
	private final Point base;
	private final RegionReference region;
	private WeakReference<Chunk> chunk;

	public ChunkReference(Chunk referent) {
		this.chunk = new WeakReference<>(referent);
		this.region = new RegionReference(referent.getRegion());
		this.base = referent.getBase();
	}

	public ChunkReference(Point base) {
		this.chunk = null;
		this.region = new RegionReference(new Point(base.getWorld(), base.getFloorX() >> Region.BLOCKS.BITS, base.getFloorY() >> Region.BLOCKS.BITS, base.getFloorZ() >> Region.BLOCKS.BITS));
		this.base = base;
	}

	public Chunk get() {
		Chunk get = chunk == null ? null : chunk.get();
		if (get != null) {
			if (!get.isLoaded()) {
				chunk = null;
				return null;
			}
		}
		return get;
	}

	public Chunk refresh(LoadOption opt) {
		Chunk newChunk = get();
		if (newChunk != null) return newChunk;

		Region newRegion = region.refresh(opt);
		if (newRegion == null) return null;

		newChunk = newRegion.getChunkFromBlock(base, opt);
		this.chunk = newChunk == null ? null : new WeakReference<>(newChunk);
		return newChunk;
	}

	@Override
	public int hashCode() {
		return base.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ChunkReference) {
			return base.equals(((ChunkReference) obj).base);
		}
		return false;
	}

	public Point getBase() {
		return base;
	}
}
