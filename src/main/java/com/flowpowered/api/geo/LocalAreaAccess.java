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

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.material.block.BlockFace;

public interface LocalAreaAccess {
    /**
     * Gets a neighbouring region.  Only the 3x3x3 cube of regions centered on this region can be obtained by this method.
     */
    public Region getLocalRegion(BlockFace face, LoadOption loadopt);

    /**
     * Gets a neighbouring region.  The coordinates provided range from 0 to 2, rather than -1 to +1. If all 3 coordinates are 1, then this region is returned.
     */
    public Region getLocalRegion(int dx, int dy, int dz, LoadOption loadopt);

    /**
     * Gets a chunk relative to a given chunk.  The given chunk must be in this region and the requested chunk must be in the 3x3x3 cube of regions centred on this region.<br>
     */
    public Chunk getLocalChunk(Chunk c, BlockFace face, LoadOption loadopt);

    /**
     * Gets a chunk relative to a given chunk.  The given chunk must be in this region and the requested chunk must be in the 3x3x3 cube of regions centred on this region.<br> <br> (ox, oy, oz) is the
     * offset to the desired chunk.  The coordinates of the offset can not have a magnitude greater than 16.
     */
    public Chunk getLocalChunk(Chunk c, int ox, int oy, int oz, LoadOption loadopt);

    /**
     * Gets a chunk relative to given chunk coordinates.  The given chunk must be in this region and the requested chunk must be in the 3x3x3 cube of regions centred on this region.<br> <br> (x, y, z)
     * are the coordinates of a chunk in this region.<br> <br> (ox, oy, oz) is the offset to the desired chunk.  The coordinates of the offset can not have a magnitude greater than 16.
     */
    public Chunk getLocalChunk(int x, int y, int z, int ox, int oy, int oz, LoadOption loadopt);

    /**
     * Gets a chunk in the 3x3x3 cube of regions centered on this region.<br> <br> The valid range for the (x, y, z) coordinates is -16 to 31.<br> <br> To request a chunk in this region, all three
     * coordinates must be in the range of 0 to 15.
     */
    public Chunk getLocalChunk(int x, int y, int z, LoadOption loadopt);
}
