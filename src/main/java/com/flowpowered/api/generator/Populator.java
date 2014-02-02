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

import java.util.Random;

import com.flowpowered.api.geo.cuboid.Chunk;

/**
 * Represents a populator for a chunk
 */
public abstract class Populator {
    private boolean needsClearance;

    public Populator() {
        this(false);
    }

    public Populator(boolean needsClearance) {
        this.needsClearance = needsClearance;
    }

    public boolean needsClearance() {
        return needsClearance;
    }

    /**
     * Populates the chunk.
     *
     * This method may make full use of the block modifying methods of the API.
     *
     * This method will be called once per chunk and it is guaranteed that a 2x2x2 cube of chunks containing the chunk will be loaded.
     *
     * The chunk to populate is the chunk with the lowest x, y and z coordinates of the cube.
     *
     * This allows the populator to create features that cross chunk boundaries.
     *
     * @param chunk the chunk to populate
     * @param random The RNG for this chunk
     */
    public abstract void populate(Chunk chunk, Random random);
}
