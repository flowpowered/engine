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
package com.flowpowered.api.geo;

import com.flowpowered.api.material.BlockMaterial;

public interface AreaBlockSource {
    /**
     * Gets the material for the block at (x, y, z)
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @return the block's material from the snapshot
     */
    public BlockMaterial getBlockMaterial(int x, int y, int z);

    /**
     * Gets the packed BlockFullData for the block at (x, y, z). Handler methods are provided by the BlockFullState class.
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @return the block's full state from the snapshot
     */
    public int getBlockFullState(int x, int y, int z);

    /**
     * Gets the data for the block at (x, y, z)
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @return the block's data from the snapshot
     */
    public short getBlockData(int x, int y, int z);
}