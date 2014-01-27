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
package com.flowpowered.api.geo.cuboid.reference;

import java.lang.ref.WeakReference;

import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.geo.discrete.Point;

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
		this.region = new RegionReference(new Point(base.getWorld(), base.getBlockX() >> Region.BLOCKS.BITS, base.getBlockY() >> Region.BLOCKS.BITS, base.getBlockZ() >> Region.BLOCKS.BITS));
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

		newChunk = newRegion.getChunkFromBlock(base.getVector(), opt);
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
