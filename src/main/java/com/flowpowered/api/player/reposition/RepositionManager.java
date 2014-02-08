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

import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.math.vector.Vector3f;

public interface RepositionManager {
	/**
	 * Gets the converted Chunk x value for the given Chunk x value
	 *
	 * @param cX the chunk x value
	 */
	public int convertChunkX(int x);

	/**
	 * Gets the converted Chunk y value for the given Chunk y value
	 *
	 * @param cY the server-side chunk y value
	 */
	public int convertChunkY(int y);

	/**
	 * Gets the converted Chunk z value for the given Chunk z value
	 *
	 * @param cZ the server-side chunk z value
	 */
	public int convertChunkZ(int z);

	/**
	 * Gets the converted x value for the given x value.  The change must be exactly an integer number of chunks.
	 *
	 * @param x the x value
	 */
	public int convertX(int x);

	/**
	 * Gets the converted y value for the given y value.  The change must be exactly an integer number of chunks.
	 *
	 * @param y the y value
	 */
	public int convertY(int y);

	/**
	 * Gets the converted z value for the given z value.  The change must be exactly an integer number of chunks.
	 *
	 * @param z the z value
	 */
	public int convertZ(int z);

	/**
	 * Gets the converted x value for the given x value.  The change must be exactly an integer number of chunks.
	 *
	 * @param x the x value
	 */
	public float convertX(float x);

	/**
	 * Gets the converted y value for the given y value.  The change must be exactly an integer number of chunks.
	 *
	 * @param y the y value
	 */
	public float convertY(float y);

	/**
	 * Gets the converted z value for the given y value.  The change must be exactly an integer number of chunks.
	 *
	 * @param z the z value
	 */
	public float convertZ(float z);

	/**
	 * Gets the converted x value for the given x value.  The change must be exactly an integer number of chunks.
	 *
	 * @param x the x value
	 */
	public double convertX(double x);

	/**
	 * Gets converted y value for the given y value.  The change must be exactly an integer number of chunks.
	 *
	 * @param y the y value
	 */
	public double convertY(double y);

	/**
	 * Gets the converted z value for the given z value.  The change must be exactly an integer number of chunks.
	 *
	 * @param z the z value
	 */
	public double convertZ(double z);

	/**
	 * Gets the converted Transform for the given Transform.  The change must be exactly an integer number of chunks in each dimension.
	 *
	 * @param t the transform
	 */
	public Transform convert(Transform t);

	/**
	 * Gets the converted Point for the given Point.  The change must be exactly an integer number of chunks in each dimension.
	 *
	 * @param p the point
	 */
	public Point convert(Point p);

	/**
	 * Gets the converted Vector3 for the given Vector3.  The change must be exactly an integer number of chunks in each dimension.
	 *
	 * @param p the point
	 */
	public Vector3f convert(Vector3f p);

	/**
	 * Gets the inverse RepositionManager that reverses changes made by this manager.
	 */
	public RepositionManager getInverse();
}
