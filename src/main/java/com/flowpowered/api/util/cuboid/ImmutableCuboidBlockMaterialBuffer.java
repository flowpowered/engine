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
package com.flowpowered.api.util.cuboid;

import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.api.util.cuboid.procedure.CuboidBlockMaterialProcedure;

public class ImmutableCuboidBlockMaterialBuffer extends CuboidBuffer {
    protected final short[] id;
    protected final short[] data;

    public ImmutableCuboidBlockMaterialBuffer(CuboidBlockMaterialBuffer buffer) {
        super(buffer.getBase().getFloorX(), buffer.getBase().getFloorY(), buffer.getBase().getFloorZ(), buffer.getSize().getFloorX(), buffer.getSize().getFloorY(), buffer.getSize().getFloorZ());
        this.id = new short[buffer.id.length];
        this.data = new short[buffer.data.length];
        System.arraycopy(buffer.id, 0, this.id, 0, buffer.id.length);
        System.arraycopy(buffer.data, 0, this.data, 0, buffer.data.length);
    }

    public ImmutableCuboidBlockMaterialBuffer(int baseX, int baseY, int baseZ, int sizeX, int sizeY, int sizeZ, short[] id, short[] data) {
        super(baseX, baseY, baseZ, sizeX, sizeY, sizeZ);
        this.id = id;
        this.data = data;
    }

    public ImmutableCuboidBlockMaterialBuffer(int baseX, int baseY, int baseZ, int sizeX, int sizeY, int sizeZ) {
        this(baseX, baseY, baseZ, sizeX, sizeY, sizeZ, new short[sizeX * sizeY * sizeZ], new short[sizeX * sizeY * sizeZ]);
    }

    public ImmutableCuboidBlockMaterialBuffer(double baseX, double baseY, double baseZ, double sizeX, double sizeY, double sizeZ) {
        this((int) baseX, (int) baseY, (int) baseZ, (int) sizeX, (int) sizeY, (int) sizeZ, new short[(int) (sizeX * sizeY * sizeZ)], new short[(int) (sizeX * sizeY * sizeZ)]);
    }

    public ImmutableCuboidBlockMaterialBuffer(Vector3f base, Vector3f size) {
        this((int) base.getX(), (int) base.getY(), (int) base.getZ(), (int) size.getX(), (int) size.getY(), (int) size.getZ(), new short[(int) (size.getX() * size.getY() * size.getZ())], new short[(int) (size.getX() * size.getY() * size.getZ())]);
    }

    @Override
    public void copyElement(int thisIndex, int sourceIndex, int runLength) {
        throw new UnsupportedOperationException("This buffer is immutable");
    }

    @Override
    public void setSource(CuboidBuffer source) {
    }

    public BlockMaterial get(int x, int y, int z) {
        int index = getIndex(x, y, z);
        if (index < 0) {
            throw new IllegalArgumentException("Coordinate (" + x + ", " + y + ", " + z + ") is outside the buffer");
        }

        return BlockMaterial.get(id[index], data[index]);
    }

    public short getId(int x, int y, int z) {
        int index = getIndex(x, y, z);
        if (index < 0) {
            throw new IllegalArgumentException("Coordinate (" + x + ", " + y + ", " + z + ") is outside the buffer");
        }
        return id[index];
    }

    public short getData(int x, int y, int z) {
        int index = getIndex(x, y, z);
        if (index < 0) {
            throw new IllegalArgumentException("Coordinate (" + x + ", " + y + ", " + z + ") is outside the buffer");
        }

        return data[index];
    }

    public void forEach(CuboidBlockMaterialProcedure procedure) {
        int index = 0;
        for (int y = baseY; y < topY; y++) {
            for (int z = baseZ; z < topZ; z++) {
                for (int x = baseX; x < topX; x++) {
                    if (!procedure.execute(x, y, z, id[index], data[index])) {
                        return;
                    }
                    index++;
                }
            }
        }
    }

    public short[] getRawId() {
        return id;
    }

    public short[] getRawData() {
        return data;
    }
}
