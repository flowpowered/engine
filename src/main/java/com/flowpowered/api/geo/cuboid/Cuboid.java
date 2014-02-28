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

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.flowpowered.api.geo.WorldSource;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.reference.WorldReference;
import com.flowpowered.math.vector.Vector3f;

/**
 * Represents a Cuboid shaped volume that is located somewhere in a world.
 */
public class Cuboid implements WorldSource {
    protected final Point base;
    protected final Vector3f size;
    protected final Vector3f position;
    private final float x;
    private final float y;
    private final float z;
    /**
     * Hashcode caching
     */
    private volatile boolean hashed = false;
    private volatile int hashcode = 0;
    /**
     * Vertex cache
     */
    private Vector3f[] vertices = null;

    /**
     * Constructs a cubiod with the point as the base point, and
     */
    public Cuboid(Point base, Vector3f size) {
        this.base = base;
        this.size = size;
        this.x = base.getVector().getX() / size.getX();
        this.y = base.getVector().getY() / size.getY();
        this.z = base.getVector().getZ() / size.getZ();
        this.position = new Vector3f(x, y, z);
    }

    public Point getBase() {
        return base;
    }

    public Vector3f getSize() {
        return size;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    @Override
    public WorldReference getWorld() {
        return base.getWorld();
    }

    /**
     * Returns the vertices of this Cuboid.
     *
     * @return The vertices
     */
    public Vector3f[] getVertices() {
        if (vertices == null) {
            vertices = new Vector3f[8];

            Vector3f base = this.base.getVector();
            // Front
            vertices[0] = new Vector3f(base.getX(), base.getY(), base.getZ() + size.getZ());
            vertices[1] = new Vector3f(base.getX() + size.getX(), base.getY(), base.getZ() + size.getZ());
            vertices[2] = new Vector3f(base.getX() + size.getX(), base.getY() + size.getY(), base.getZ() + size.getZ());
            vertices[3] = new Vector3f(base.getX(), base.getY() + size.getY(), base.getZ() + size.getZ());
            // Back
            vertices[4] = new Vector3f(base.getX(), base.getY(), base.getZ());
            vertices[5] = new Vector3f(base.getX() + size.getX(), base.getY(), base.getZ());
            vertices[6] = new Vector3f(base.getX() + size.getX(), base.getY() + size.getY(), base.getZ());
            vertices[7] = new Vector3f(base.getX(), base.getY() + size.getY(), base.getZ());
        }

        return vertices;
    }

    public boolean contains(Vector3f vec) {
        Vector3f base = this.base.getVector();
        Vector3f max = base.add(size);
        return (base.getX() <= vec.getX() && vec.getX() < max.getX()) && (base.getY() <= vec.getY() && vec.getY() < max.getY()) && (base.getZ() <= vec.getZ() && vec.getZ() < max.getZ());
    }

    @Override
    public int hashCode() {
        if (!hashed) {
            hashcode = new HashCodeBuilder(563, 21).append(base).append(size).toHashCode();
            hashed = true;
        }
        return hashcode;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        } else if (!(obj instanceof Cuboid)) {
            return false;
        } else {
            Cuboid cuboid = (Cuboid) obj;

            return cuboid.size.getX() == size.getX() && cuboid.size.getY() == size.getY() && cuboid.size.getZ() == size.getZ() && cuboid.getWorld().equals(getWorld()) && cuboid.getX() == getX() && cuboid.getY() == getY() && cuboid.getZ() == getZ();
        }
    }

    @Override
    public String toString() {
        return "Cuboid[" + size.getX() + ", " + size.getY() + ", " + size.getZ() + "]@[" + getX() + ", " + getY() + ", " + getZ() + "]";
    }
}
