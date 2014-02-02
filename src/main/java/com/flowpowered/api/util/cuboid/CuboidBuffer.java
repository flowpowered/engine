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

import com.flowpowered.api.geo.cuboid.Chunk;

import com.flowpowered.math.vector.Vector3f;

/**
 * This class implements a Cuboid common methods for a one dimensional array Cuboid Buffer
 *
 * Elements are stored in column order and each column is +1 on the Z dimension relative to the previous one.
 *
 * Each YZ plane is followed by the plane corresponding to +1 on the X dimension.
 *
 * It is assumed that the Cuboid has dimensions (SX, SY, SZ) and the base is set at the origin.
 *
 * buffer[0] = data(0, 0, 0 ) buffer[1] = data(0, 1, 0 ) ..... buffer[SY-1] = data(0, SY-1, 0 ) buffer[SY] = data(0, 0 1 ) .... buffer[SZ*SY - 1] = data(0, SY-1, SZ-1) buffer[SZ*SY] = data(1, 0, 0 )
 * .... buffer[SZ*SY*SX -1] = data(SX-1, SY-1, SZ-1)
 *
 * TODO is this the best package to put this?
 */
public abstract class CuboidBuffer {
    protected final Vector3f size;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final Vector3f base;
    protected final int baseX;
    protected final int baseY;
    protected final int baseZ;
    /*
     * Note: These values are not actually within the cuboid The cuboid goes
     * from baseX to baseX + sizeX - 1 top = base + size
     */
    protected final Vector3f top;
    protected final int topX;
    protected final int topY;
    protected final int topZ;
    protected final int Xinc;
    protected final int Yinc;
    protected final int Zinc;

    protected CuboidBuffer(int baseX, int baseY, int baseZ, int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;

        this.size = new Vector3f(sizeX, sizeY, sizeZ);

        this.baseX = baseX;
        this.baseY = baseY;
        this.baseZ = baseZ;

        this.base = new Vector3f(baseX, baseY, baseZ);

        this.topX = baseX + sizeX;
        this.topY = baseY + sizeY;
        this.topZ = baseZ + sizeZ;

        this.top = new Vector3f(this.topX, this.topY, this.topZ);

        Yinc = sizeZ * (Zinc = sizeX * (Xinc = 1));
    }

    protected CuboidBuffer(double baseX, double baseY, double baseZ, double sizeX, double sizeY, double sizeZ) {
        this((int) baseX, (int) baseY, (int) baseZ, (int) sizeX, (int) sizeY, (int) sizeZ);
    }

    protected CuboidBuffer(Vector3f base, Vector3f size) {
        this(base.getX(), base.getY(), base.getZ(), size.getX(), size.getY(), size.getZ());
    }

    /**
     * Gets a Point representing the base of this CuboidBuffer
     */
    public Vector3f getBase() {
        return base;
    }

    /**
     * Gets the X-coordinate of the chunk the base of this CuboidBuffer is in
     *
     * @return base chunk X-coordinate
     */
    public int getBaseChunkX() {
        return baseX >> Chunk.BLOCKS.BITS;
    }

    /**
     * Gets the Y-coordinate of the chunk the base of this CuboidBuffer is in
     *
     * @return base chunk Y-coordinate
     */
    public int getBaseChunkY() {
        return baseY >> Chunk.BLOCKS.BITS;
    }

    /**
     * Gets the Z-coordinate of the chunk the base of this CuboidBuffer is in
     *
     * @return base chunk Z-coordinate
     */
    public int getBaseChunkZ() {
        return baseZ >> Chunk.BLOCKS.BITS;
    }

    /**
     * Gets the size of the CuboidBuffer
     */
    public Vector3f getSize() {
        return size;
    }

    /**
     * Gets the volume of the CuboidBuffer
     */
    public int getVolume() {
        return sizeX * sizeY * sizeZ;
    }

    /**
     * Gets the top-coordinates of the CuboidBuffer, these are outside this buffer<br> These coordinates are an addition of base and size
     */
    public Vector3f getTop() {
        return top;
    }

    /**
     * Return true if the coordinates are inside the buffer.
     *
     * @param x The x coordinate to check.
     * @param y The y coordinate to check.
     * @param z The Z coordinate to check.
     * @return True if the coordinate are in the buffer, false if not.
     */
    public boolean isInside(int x, int y, int z) {
        return getIndex(x, y, z) >= 0;
    }

    /**
     * Copies the data contained within the given CuboidShortBuffer to this one. Any non-overlapping locations are ignored
     *
     * @param source The CuboidShortBuffer source from which to copy the data.
     */
    public void write(CuboidBuffer source) {
        CuboidBufferCopyRun run = new CuboidBufferCopyRun(source, this);

        int sourceIndex = run.getBaseSource();
        int thisIndex = run.getBaseTarget();
        int runLength = run.getLength();
        int innerRepeats = run.getInnerRepeats();
        int outerRepeats = run.getOuterRepeats();

        setSource(source);

        if (!(sourceIndex == -1 || thisIndex == -1)) {
            for (int x = 0; x < outerRepeats; x++) {
                int outerSourceIndex = sourceIndex;
                int outerThisIndex = thisIndex;
                for (int z = 0; z < innerRepeats; z++) {
                    copyElement(outerThisIndex, outerSourceIndex, runLength);

                    outerSourceIndex += source.Zinc;
                    outerThisIndex += Zinc;
                }
                sourceIndex += source.Yinc;
                thisIndex += Yinc;
            }
        }
    }

    protected int getIndex(int x, int y, int z) {
        return getIndex(this, x, y, z);
    }

    protected static int getIndex(CuboidBuffer source, int x, int y, int z) {
        if (x < source.baseX || x >= source.topX || y < source.baseY || y >= source.topY || z < source.baseZ || z >= source.topZ) {
            return -1;
        }

        return (y - source.baseY) * source.Yinc + (z - source.baseZ) * source.Zinc + (x - source.baseX) * source.Xinc;
    }

    protected CuboidBufferCopyRun getCopyRun(CuboidBuffer other) {
        return new CuboidBufferCopyRun(this, other);
    }

    public abstract void copyElement(int thisIndex, int sourceIndex, int runLength);

    public abstract void setSource(CuboidBuffer source);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{Buffer Size=" + sizeX * sizeY * sizeZ + ", Base=(" + baseX + ", " + baseY + ", " + baseZ + "}, Size=(" + sizeX + ", " + sizeY + ", " + sizeZ + "), " + "Increments=(" + Xinc + ", " + Yinc + ", " + Zinc + "), Top=(" + topX + ", " + topY + ", " + topZ + ")}";
    }

    protected static class CuboidBufferCopyRun {
        private int overlapBaseX;
        private int overlapBaseY;
        private int overlapBaseZ;
        private int overlapSizeX;
        private int overlapSizeY;
        private int overlapSizeZ;
        private int sourceIndex;
        private int targetIndex;

        public CuboidBufferCopyRun(CuboidBuffer source, CuboidBuffer target) {
            overlapBaseX = Math.max(source.baseX, target.baseX);
            overlapBaseY = Math.max(source.baseY, target.baseY);
            overlapBaseZ = Math.max(source.baseZ, target.baseZ);

            overlapSizeX = Math.min(source.topX, target.topX) - overlapBaseX;
            overlapSizeY = Math.min(source.topY, target.topY) - overlapBaseY;
            overlapSizeZ = Math.min(source.topZ, target.topZ) - overlapBaseZ;

            if (overlapSizeX < 0 || overlapSizeY < 0 || overlapSizeZ < 0) {
                sourceIndex = -1;
                targetIndex = -1;
                return;
            }

            sourceIndex = getIndex(source, overlapBaseX, overlapBaseY, overlapBaseZ);
            targetIndex = getIndex(target, overlapBaseX, overlapBaseY, overlapBaseZ);
        }

        public int getBaseSource() {
            return sourceIndex;
        }

        public int getBaseTarget() {
            return targetIndex;
        }

        public int getLength() {
            return overlapSizeX;
        }

        public int getInnerRepeats() {
            return overlapSizeZ;
        }

        public int getOuterRepeats() {
            return overlapSizeY;
        }
    }
}
