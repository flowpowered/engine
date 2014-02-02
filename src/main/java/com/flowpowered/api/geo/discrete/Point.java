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
import java.io.Serializable;
import java.lang.reflect.Field;

import com.flowpowered.api.Flow;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.math.vector.Vector3f;

public class Point implements Serializable {
    public static final Point INVALID = new Point(null, Vector3f.ZERO);
    private static final Field worldField = getWorldField();

    private final transient World world;
    private final Vector3f vector;

    public Point(World world, float x, float y, float z) {
        this.world = world;
        this.vector = new Vector3f(x, y, z);
    }

    public Point(World world, Vector3f vector) {
        this.world = world;
        this.vector = vector;
    }

    public World getWorld() {
        return world;
    }

    public Vector3f getVector() {
        return vector;
    }

    public int getBlockX() {
        return vector.getFloorX();
    }

    public int getBlockY() {
        return vector.getFloorY();
    }

    public int getBlockZ() {
        return vector.getFloorZ();
    }

    public int getChunkX() {
        return getBlockX() >> Chunk.BLOCKS.BITS;
    }

    public int getChunkY() {
        return getBlockY() >> Chunk.BLOCKS.BITS;
    }

    public int getChunkZ() {
        return getBlockZ() >> Chunk.BLOCKS.BITS;
    }

    public Chunk getChunk(LoadOption loadopt) {
        return world.getChunk(getChunkX(), getChunkY(), getChunkZ(), loadopt);
    }

    public Region getRegion(LoadOption loadopt) {
        return world.getRegionFromChunk(getChunkX(), getChunkY(), getChunkZ(), loadopt);
    }

    public String toBlockString() {
        return "{" + world.getName() + ":" + getBlockX() + ", " + getBlockY() + ", " + getBlockZ() + "}";
    }

    public String toChunkString() {
        return "{" + world.getName() + ":" + getChunkX() + ", " + getChunkY() + ", " + getChunkZ() + "}";
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(world != null ? world.getName() : "null");
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        String world = in.readUTF();
        World w = Flow.getEngine().getWorldManager().getWorld(world, true);
        if (worldField != null) {
            try {
                worldField.set(this, w);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                if (Flow.getEngine().debugMode()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Field getWorldField() {
        Field field = null;
        try {
            field = Point.class.getDeclaredField("world");
            field.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        return field;
    }
}