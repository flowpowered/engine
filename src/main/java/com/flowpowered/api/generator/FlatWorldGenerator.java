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
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;

/**
 * Generates a flat world of a material
 */
public class FlatWorldGenerator implements WorldGenerator {
    private final BlockMaterial material;

    public FlatWorldGenerator() {
        material = BlockMaterial.SOLID_BLUE;
    }

    public FlatWorldGenerator(BlockMaterial material) {
        this.material = material;
    }

    @Override
    public void generate(CuboidBlockMaterialBuffer blockData, World world) {
        int flooredY = blockData.getBase().getFloorY();
        if (flooredY < 0) {
            blockData.setHorizontalLayer(flooredY, (blockData.getSize().getFloorY() / 2), material);
            blockData.flood(material);
        } else {
            blockData.flood(BlockMaterial.AIR);
        }
    }

    @Override
    public Populator[] getPopulators() {
        return new Populator[0];
    }

    @Override
    public String getName() {
        return "FlatWorld";
    }
}
