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
package org.spout.engine.entity;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.flowpowered.math.vector.Vector3i;

/**
 * An Iterator that iterates outwards from a given central 3d integer coordinate.<br> <br> The Manhattan distance from the given center to the coordinates in the sequence increases monotonically and
 * the iterator passes through all integer coordinates.
 */
public class OutwardIterator implements Iterator<Vector3i>, Serializable {
	private static final long serialVersionUID = 1L;
	private final Vector3i center;
	private Vector3i step;
    private Vector3i current;
	private int distance;
	private int endDistance;
	private boolean hasNext;
	private boolean first = true;

	public OutwardIterator() {
		this(0, 0, 0);
	}

	public OutwardIterator(int x, int y, int z) {
		this(x, y, z, Integer.MAX_VALUE);
	}

	public OutwardIterator(int x, int y, int z, int maxDistance) {
        current = new Vector3i(x, y, z);
		center = new Vector3i(x, y, z);
		step = new Vector3i(0, 0, 0);
		first = true;
		distance = 0;
		this.endDistance = maxDistance;
		this.hasNext = true;
	}

	/**
	 * Resets the iterator and positions it at (x, y, z)
	 */
	public void reset(int x, int y, int z) {
        current = new Vector3i(x, y, z);
		first = true;
		hasNext = true;
		distance = 0;
	}

	public void reset(int x, int y, int z, int startDistance, int endDistance) {
		this.endDistance = endDistance;
		reset(x, y, z);
		if (startDistance > endDistance) {
			this.hasNext = false;
		} else if (startDistance > 0) {
			// reset to start at distance
			y = y + startDistance - 1;
			first = false;
		}
	}

	public void reset(int x, int y, int z, int endDistance) {
		reset(x, y, z, 0, endDistance);
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public Vector3i next() {
		if (!this.hasNext) {
			throw new NoSuchElementException("The Outward Iterator ran out of elements");
		}
		// First block is always the central block
		if (first) {
            step = new Vector3i(0, step.getY(), 0);
			first = false;
			if (this.endDistance <= 0) {
				this.hasNext = false;
			}
		} else {
			int dx = current.getX() - center.getX();
			int dy = current.getY() - center.getY();
			int dz = current.getZ() - center.getZ();

			// Last block was top of layer, move to start of next layer
			if (dx == 0 && dz == 0 && dy >= 0) {
				current = new Vector3i(current.getX(), (center.getY() << 1) - current.getY() - 1, current.getZ());
				step = new Vector3i(0, step.getY(), 0);
				distance++;
			} else if (dx == 0) {
				// Reached end of horizontal slice
				// Move up to next slice
				if (dz >= 0) {
                    step = new Vector3i(1, step.getY(), -1);
                    current = current.add(0, 1, 0);

					// Bottom half of layer
					if (dy < 0) {
                        current = current.add(0, 0, 1);
						// Top half of layer
					} else {
                        current = current.add(0, 0, -1);
						// Reached top of layer
						if (current.getZ() == center.getZ()) {
                            step = new Vector3i(0, step.getY(), 0);
						}
					}
					// Change direction (50% of horizontal slice complete)
				} else {
                    step = new Vector3i(-1, step.getY(), 1);
				}
			} else if (dz == 0) {
				// Change direction (25% of horizontal slice complete)
				if (dx > 0) {
                    step = new Vector3i(-1, step.getY(), -1);
					// Change direction (75% of horizontal slice compete)
				} else {
                    step = new Vector3i(1, step.getY(), 1);
				}
			}
            current = current.add(step);
			if (distance == 0 || (dx == 0 && dz == 1 && dy >= endDistance - 1)) {
				hasNext = false;
			}
		}
        return current;
	}

	public int getDistance() {
		return distance;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This operation is not supported");
	}
}
