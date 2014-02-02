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

import java.util.Collection;

import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.math.vector.Vector3f;

public interface AreaRegionAccess extends AreaChunkAccess {
    /**
     * Gets an unmodifiable collection of all loaded regions
     *
     * @return all loaded regions
     */
    public Collection<Region> getRegions();

    /**
     * Gets the {@link Region} at region coordinates (x, y, z)
     *
     * @param x the region x coordinate
     * @param y the region y coordinate
     * @param z the region z coordinate
     * @param loadopt to control whether to load and/or generate the region, if needed
     * @return the region
     */
    public Region getRegion(int x, int y, int z, LoadOption loadopt);

    /**
     * Gets the {@link Region} at chunk coordinates (x, y, z)
     *
     * @param x the chunk x coordinate
     * @param y the chunk y coordinate
     * @param z the chunk z coordinate
     * @param loadopt to control whether to load and/or generate the region, if needed
     * @return the region
     */
    public Region getRegionFromChunk(int x, int y, int z, LoadOption loadopt);

    /**
     * Gets the {@link Region} at block coordinates (x, y, z)
     *
     * @param x the block x coordinate
     * @param y the block y coordinate
     * @param z the block z coordinate
     * @param loadopt to control whether to load and/or generate the region, if needed
     * @return the region
     */
    public Region getRegionFromBlock(int x, int y, int z, LoadOption loadopt);

    /**
     * Gets the {@link Region} at block coordinates (x, y, z)
     *
     * @param position of the block
     * @param loadopt to control whether to load and/or generate the region, if needed
     * @return the region
     */
    public Region getRegionFromBlock(Vector3f position, LoadOption loadopt);
}
