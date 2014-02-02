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

import java.util.Arrays;

import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.math.vector.Vector3f;

public class CuboidBlockMaterialBuffer extends ImmutableCuboidBlockMaterialBuffer {
    private CuboidBlockMaterialBuffer source;
    private final ImmutableCuboidBlockMaterialBuffer backBuffer;

    public CuboidBlockMaterialBuffer(CuboidBlockMaterialBuffer buffer) {
        this(buffer, false);
    }

    public CuboidBlockMaterialBuffer(CuboidBlockMaterialBuffer buffer, boolean backBuffer) {
        super(buffer);
        if (backBuffer) {
            this.backBuffer = new ImmutableCuboidBlockMaterialBuffer(this);
        } else {
            this.backBuffer = null;
        }
    }

    public CuboidBlockMaterialBuffer(int baseX, int baseY, int baseZ, int sizeX, int sizeY, int sizeZ, short[] id, short[] data) {
        this(baseX, baseY, baseZ, sizeX, sizeY, sizeZ, id, data, false);
    }

    public CuboidBlockMaterialBuffer(int baseX, int baseY, int baseZ, int sizeX, int sizeY, int sizeZ, short[] id, short[] data, boolean backBuffer) {
        super(baseX, baseY, baseZ, sizeX, sizeY, sizeZ, id, data);
        if (backBuffer) {
            this.backBuffer = new ImmutableCuboidBlockMaterialBuffer(this);
        } else {
            this.backBuffer = null;
        }
    }

    public CuboidBlockMaterialBuffer(int baseX, int baseY, int baseZ, int sizeX, int sizeY, int sizeZ) {
        this(baseX, baseY, baseZ, sizeX, sizeY, sizeZ, false);
    }

    public CuboidBlockMaterialBuffer(int baseX, int baseY, int baseZ, int sizeX, int sizeY, int sizeZ, boolean backBuffer) {
        super(baseX, baseY, baseZ, sizeX, sizeY, sizeZ);
        if (backBuffer) {
            this.backBuffer = new ImmutableCuboidBlockMaterialBuffer(this);
        } else {
            this.backBuffer = null;
        }
    }

    public CuboidBlockMaterialBuffer(double baseX, double baseY, double baseZ, double sizeX, double sizeY, double sizeZ) {
        this(baseX, baseY, baseZ, sizeX, sizeY, sizeZ, false);
    }

    public CuboidBlockMaterialBuffer(double baseX, double baseY, double baseZ, double sizeX, double sizeY, double sizeZ, boolean backBuffer) {
        super(baseX, baseY, baseZ, sizeX, sizeY, sizeZ);
        if (backBuffer) {
            this.backBuffer = new ImmutableCuboidBlockMaterialBuffer(this);
        } else {
            this.backBuffer = null;
        }
    }

    public CuboidBlockMaterialBuffer(Vector3f base, Vector3f size) {
        this(base, size, false);
    }

    public CuboidBlockMaterialBuffer(Vector3f base, Vector3f size, boolean backBuffer) {
        super(base, size);
        if (backBuffer) {
            this.backBuffer = new ImmutableCuboidBlockMaterialBuffer(this);
        } else {
            this.backBuffer = null;
        }
    }

    @Override
    public void copyElement(int thisIndex, int sourceIndex, int runLength) {
        final int end = thisIndex + runLength;
        for (; thisIndex < end; thisIndex++) {
            id[thisIndex] = source.id[sourceIndex];
            data[thisIndex] = source.data[sourceIndex++];
        }
    }

    @Override
    public void setSource(CuboidBuffer source) {
        if (source instanceof CuboidBlockMaterialBuffer) {
            this.source = (CuboidBlockMaterialBuffer) source;
        } else {
            throw new IllegalArgumentException("Only CuboidShortBuffers may be used as the data source when copying to a CuboidShortBuffer");
        }
    }

    /**
     * Sets a horizontal layer of blocks to a given material
     *
     * @param y - coordinate of the start of the layer
     * @param height of the layer
     * @param material to set to
     */
    public void setHorizontalLayer(int y, int height, BlockMaterial material) {
        setHorizontalLayer(y, height, material.getId(), material.getData());
    }

    /**
     * Sets a horizontal layer of blocks to a given material id and data
     *
     * @param y - coordinate of the start of the layer
     * @param height of the layer
     * @param id of the material to set to
     * @param data to set to
     */
    public void setHorizontalLayer(int y, int height, short id, short data) {
        final int startIndex = getIndex(this.baseX, y, this.baseZ);
        final int endIndex = getIndex(this.topX - 1, y + height - 1, this.topZ - 1) + 1;
        if (startIndex < 0 || endIndex <= 0) {
            throw new IllegalArgumentException("Layer Y-Coordinate (y=" + y + ", height=" + height + ") are outside the buffer");
        }
        Arrays.fill(this.id, startIndex, endIndex, id);
        Arrays.fill(this.data, startIndex, endIndex, data);
    }

    /**
     * Sets a single block material
     *
     * @param x - coordinate of the block
     * @param y - coordinate of the block
     * @param z - coordinate of the block
     * @param material to set to
     */
    public void set(int x, int y, int z, BlockMaterial material) {
        int index = getIndex(x, y, z);
        if (index < 0) {
            throw new IllegalArgumentException("Coordinate (" + x + ", " + y + ", " + z + ") is outside the buffer");
        }

        this.id[index] = material.getId();
        this.data[index] = material.getData();
    }

    /**
     * Sets a single block material id and data
     *
     * @param x - coordinate of the block
     * @param y - coordinate of the block
     * @param z - coordinate of the block
     * @param id of the material to set to
     * @param data to set to
     */
    public void set(int x, int y, int z, short id, short data) {
        int index = getIndex(x, y, z);
        if (index < 0) {
            throw new IllegalArgumentException("Coordinate (" + x + ", " + y + ", " + z + ") is outside the buffer");
        }

        this.id[index] = id;
        this.data[index] = data;
    }

    public void flood(BlockMaterial material) {
        for (int i = 0; i < id.length; i++) {
            this.id[i] = material.getId();
            this.data[i] = material.getData();
        }
    }

    @Override
    public short[] getRawId() {
        return id;
    }

    @Override
    public short[] getRawData() {
        return data;
    }

    public ImmutableCuboidBlockMaterialBuffer getBackBuffer() {
        return backBuffer == null ? this : backBuffer;
    }
}
