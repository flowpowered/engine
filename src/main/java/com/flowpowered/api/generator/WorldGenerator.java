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

import com.flowpowered.commons.Named;

import com.flowpowered.api.geo.World;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;

/**
 * Represents a World generator.
 *
 * WorldGenerators are used to generate {@link World}s (surprise surprise)
 */
public interface WorldGenerator extends Named {
    /**
     * Gets the block structure for a Chunk.
     *
     * The CuboidBuffer will always be chunk-aligned, and could be of a variable (chunk) size.<br><br> Use {@link CuboidBlockMaterialBuffer#getBase()} and {@link CuboidBlockMaterialBuffer#getTop()} to
     * obtain the Block bounds in which can be generated.
     *
     * It is recommended that seeded random number generators from WorldGeneratorUtils are used.
     *
     * @param blockData a zeroed CuboidBuffer which has to be fully generated
     * @param world in which is generated
     */
    public void generate(CuboidBlockMaterialBuffer blockData, World world);

    /**
     * Gets an array of Populators for the world generator
     *
     * @return the Populator array
     */
    public Populator[] getPopulators();

    /**
     * Gets the name of the generator. This name should be unique to prevent two generators overwriting the same world
     */
    @Override
    public String getName();
}
