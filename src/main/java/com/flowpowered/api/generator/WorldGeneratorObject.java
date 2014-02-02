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
package com.flowpowered.api.generator;

import com.flowpowered.api.geo.World;

/**
 * Represents an Object for a WorldGenerator
 */
public abstract class WorldGeneratorObject {
    /**
     * Verify if the object can be placed at the given coordinates.
     *
     * @param w The world w.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return true if the object can be placed, false if it can't.
     */
    public abstract boolean canPlaceObject(World w, int x, int y, int z);

    /**
     * Place this object into the world at the given coordinates.
     *
     * @param w The world w.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     */
    public abstract void placeObject(World w, int x, int y, int z);

    /**
     * Attempts placement of this object into the world at the given coordinates. The attempt will fail if {@link #canPlaceObject(com.flowpowered.api.geo.World, int, int, int)} returns false.
     *
     * @param w The world w.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return True if the object was placed, false if otherwise.
     */
    public boolean tryPlaceObject(World w, int x, int y, int z) {
        if (canPlaceObject(w, x, y, z)) {
            placeObject(w, x, y, z);
            return true;
        }
        return false;
    }
}
