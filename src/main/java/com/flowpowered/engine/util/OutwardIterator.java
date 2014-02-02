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
package com.flowpowered.engine.util;

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
