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
package com.flowpowered.api.player.reposition;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.math.vector.Vector3f;


public abstract class RepositionManagerImpl implements RepositionManager {
	@Override
	public int convertChunkX(int x) {
		return (convertX(x << Chunk.BLOCKS.BITS)) >> Chunk.BLOCKS.BITS;
	}

	@Override
	public int convertChunkY(int y) {
		return (convertY(y << Chunk.BLOCKS.BITS)) >> Chunk.BLOCKS.BITS;
	}

	@Override
	public int convertChunkZ(int z) {
		return (convertZ(z << Chunk.BLOCKS.BITS)) >> Chunk.BLOCKS.BITS;
	}

	@Override
	public int convertX(int x) {
		return (int) convertX((double) x);
	}

	@Override
	public int convertY(int y) {
		return (int) convertY((double) y);
	}

	@Override
	public int convertZ(int z) {
		return (int) convertZ((double) z);
	}

	@Override
	public float convertX(float x) {
		return (float) convertX((double) x);
	}

	@Override
	public float convertY(float y) {
		return (float) convertY((double) y);
	}

	@Override
	public float convertZ(float z) {
		return (float) convertZ((double) z);
	}

	@Override
	public abstract double convertX(double x);

	@Override
	public abstract double convertY(double y);

	@Override
	public abstract double convertZ(double z);

	@Override
	public Transform convert(Transform t) {
		return new Transform(convert(t.getPosition()), t.getRotation(), t.getScale());
	}

	@Override
	public Point convert(Point p) {
		Point newP = new Point(p.getWorld(), convert(p.getVector()));
		return newP;
	}

	@Override
	public Vector3f convert(Vector3f v) {
		float newX = convertX(v.getX());
		float newY = convertY(v.getY());
		float newZ = convertZ(v.getZ());
		Vector3f newP = new Vector3f(newX, newY, newZ);
		return newP;
	}
}
