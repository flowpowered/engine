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
package com.flowpowered.api.geo.discrete;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.flowpowered.commons.StringUtil;

import com.flowpowered.api.Flow;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldSource;
import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.api.geo.cuboid.Region;

/**
 * Represents a position in a World
 */
public class Point extends Vector3f implements WorldSource {
	private static final long serialVersionUID = 1L;
	protected final World world;
	public static final Point invalid = new Point(null, 0, 0, 0);
	/**
	 * Hashcode caching
	 */
	private transient volatile boolean hashed = false;
	private transient volatile int hashcode = 0;

	public Point(Point point) {
		super(point);
		world = point.getWorld();
	}

	public Point(Vector3f vector, World w) {
		super(vector);
		world = w;
	}

	public Point(World world, float x, float y, float z) {
		super(x, y, z);
		this.world = world;
	}

	@Override
	public Point div(float val) {
		return new Point(super.div(val), world);
	}

	@Override
	public Point div(double val) {
		return new Point(super.div(val), world);
	}

	@Override
	public Point div(Vector3f other) {
		return new Point(super.div(other), world);
	}

	@Override
	public Point div(double x, double y, double z) {
		return new Point(super.div(x, y, z), world);
	}

	@Override
	public Point div(float x, float y, float z) {
		return new Point(super.div(x, y, z), world);
	}

	@Override
	public Point mul(float val) {
		return new Point(super.mul(val), world);
	}

	@Override
	public Point mul(double val) {
		return new Point(super.mul(val), world);
	}

	@Override
	public Point mul(Vector3f other) {
		return new Point(super.mul(other), world);
	}

	@Override
	public Point mul(double x, double y, double z) {
		return new Point(super.mul(x, y, z), world);
	}

	@Override
	public Point mul(float x, float y, float z) {
		return new Point(super.mul(x, y, z), world);
	}

	public Point add(Point other) {
		if (world != other.world) {
			throw new IllegalArgumentException("Cannot add two points in seperate worlds");
		}
		return new Point(super.add(other), world);
	}

	@Override
	public Point add(Vector3f other) {
		return new Point(super.add(other), world);
	}

	@Override
	public Point add(float x, float y, float z) {
		return new Point(super.add(x, y, z), world);
	}

	@Override
	public Point add(double x, double y, double z) {
		return new Point(super.add(x, y, z), world);
	}

	@Override
	public Point sub(Vector3f other) {
		return new Point(super.sub(other), world);
	}

	@Override
	public Point sub(float x, float y, float z) {
		return new Point(super.sub(x, y, z), world);
	}

	@Override
	public Point sub(double x, double y, double z) {
		return new Point(super.sub(x, y, z), world);
	}

	public int getBlockX() {
		return this.getFloorX();
	}

	public int getBlockY() {
		return this.getFloorY();
	}

	public int getBlockZ() {
		return this.getFloorZ();
	}

	public int getChunkX() {
		return this.getFloorX() >> Chunk.BLOCKS.BITS;
	}

	public int getChunkY() {
		return this.getFloorY() >> Chunk.BLOCKS.BITS;
	}

	public int getChunkZ() {
		return this.getFloorZ() >> Chunk.BLOCKS.BITS;
	}

	public Chunk getChunk(LoadOption loadopt) {
		return world.getChunk(getChunkX(), getChunkY(), getChunkZ(), loadopt);
	}

	public Region getRegion(LoadOption loadopt) {
		return world.getRegionFromChunk(getChunkX(), getChunkY(), getChunkZ(), loadopt);
	}

	/**
	 * Gets the square of the distance between two points.
	 *
	 * This will return Double.MAX_VALUE if the other Point is null, either world is null, or the two points are in different worlds.
	 *
	 * Otherwise, it returns the Manhattan distance.
	 */
	public double getSquaredDistance(Point other) {
		if (other == null || world == null || other.world == null || !world.equals(other.world)) {
			return Double.MAX_VALUE;
		}
		double dx = getX() - other.getX();
		double dy = getY() - other.getY();
		double dz = getZ() - other.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/**
	 * Gets the distance between two points.
	 *
	 * This will return Double.MAX_VALUE if the other Point is null, either world is null, or the two points are in different worlds.
	 *
	 * Otherwise, it returns the Manhattan distance.
	 */
	public double getDistance(Point other) {
		return Math.sqrt(getSquaredDistance(other));
	}

	/**
	 * Gets the Manhattan distance between two points.
	 *
	 * This will return Double.MAX_VALUE if the other Point is null, either world is null, or the two points are in different worlds.
	 *
	 * Otherwise, it returns the Manhattan distance.
	 */
	public double getManhattanDistance(Point other) {
		if (other == null || world == null || other.world == null || !world.equals(other.world)) {
			return Double.MAX_VALUE;
		}
		return Math.abs(getX() - other.getX()) + Math.abs(getY() - other.getY()) + Math.abs(getZ() - other.getZ());
	}

	/**
	 * Gets the largest distance between two points, when projected onto one of the axes.
	 *
	 * This will return Double.MAX_VALUE if the other Point is null, either world is null, or the two points are in different worlds.
	 *
	 * Otherwise, it returns the max distance.
	 */
	public double getMaxDistance(Point other) {
		if (other == null || world == null || other.world == null || !world.equals(other.world)) {
			return Double.MAX_VALUE;
		}
		return Math.max(Math.abs(getX() - other.getX()),
						Math.max(Math.abs(getY() - other.getY()),
						Math.abs(getZ() - other.getZ())));
	}

	/**
	 * Gets the world this point is locate in
	 *
	 * @return the world
	 */
	@Override
	public World getWorld() {
		return world;
	}

	/**
	 * Gets the block this point is locate in
	 *
	 * @return the world
	 */
	public Block getBlock() {
		return world.getBlock(getX(), getY(), getZ());
	}

	@Override
	public int hashCode() {
		if (!hashed) {
			hashcode = new HashCodeBuilder(5033, 61).appendSuper(super.hashCode()).append(world).toHashCode();
			hashed = true;
		}
		return hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Point)) {
			return false;
		} else {
			Point point = (Point) obj;
			boolean worldEqual = point.world == world || (point.world != null && point.world.equals(world));
			return worldEqual && point.getX() == getX() && point.getY() == getY() && point.getZ() == getZ();
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + StringUtil.toString(world, getX(), getY(), getZ());
	}

	public String toBlockString() {
		return "{" + world.getName() + ":" + getBlockX() + ", " + getBlockY() + ", " + getBlockZ() + "}";
	}

	public String toChunkString() {
		return "{" + world.getName() + ":" + getChunkX() + ", " + getChunkY() + ", " + getChunkZ() + "}";
	}

	//Custom serialization logic because world can not be made serializable
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
		out.writeUTF(world != null ? world.getName() : "null");
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
		String world = in.readUTF();
		World w = Flow.getEngine().getWorldManager().getWorld(world, true);
		try {
			Field field;

			field = Point.class.getDeclaredField("world");
			field.setAccessible(true);
			field.set(this, w);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			if (Flow.debugMode()) {
				e.printStackTrace();
			}
		}
	}
}
