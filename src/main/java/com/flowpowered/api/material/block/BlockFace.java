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
package com.flowpowered.api.material.block;

import java.io.Serializable;

import com.flowpowered.commons.bit.ByteBitMask;
import gnu.trove.map.hash.TIntObjectHashMap;

import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;

/**
 * Indicates the facing of a Block
 */
public enum BlockFace implements ByteBitMask, Serializable {
    TOP(0x1, 0, 1, 0, Quaternionf.fromAngleDegAxis(-90, 1, 0, 0)),
    BOTTOM(0x2, 0, -1, 0, Quaternionf.fromAngleDegAxis(90, 1, 0, 0), TOP),
    NORTH(0x4, -1, 0, 0, Quaternionf.fromAngleDegAxis(-90, 0, 1, 0)),
    SOUTH(0x8, 1, 0, 0, Quaternionf.fromAngleDegAxis(90, 0, 1, 0), NORTH),
    EAST(0x10, 0, 0, -1, Quaternionf.fromAngleDegAxis(180, 0, 1, 0)),
    WEST(0x20, 0, 0, 1, Quaternionf.fromAngleDegAxis(0, 0, 1, 0), EAST),
    THIS(0x40, 0, 0, 0, Quaternionf.IDENTITY);
    private final byte mask;
    private final Vector3i offset;
    private final Quaternionf direction;
    private BlockFace opposite = this;
    private static final TIntObjectHashMap<BlockFace> OFFSET_MAP = new TIntObjectHashMap<>(7);
    private static final long serialVersionUID = 1L;

    static {
        for (BlockFace face : values()) {
            OFFSET_MAP.put(getOffsetHash(face.getOffset()), face);
        }
    }

    private BlockFace(int mask, int dx, int dy, int dz, Quaternionf direction, BlockFace opposite) {
        this(mask, dx, dy, dz, direction);
        this.opposite = opposite;
        opposite.opposite = this;
    }

    private BlockFace(int mask, int dx, int dy, int dz, Quaternionf direction) {
        this.offset = new Vector3i(dx, dy, dz);
        this.direction = direction;
        this.mask = (byte) mask;
    }

    protected static byte getOffsetHash(Vector3i offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        x += 1;
        y += 1;
        z += 1;
        return (byte) (x | y << 2 | z << 4);
    }

    /**
     * Represents the rotation of the BlockFace in the world as a Quaternion. This is the rotation form the west face to this face.
     *
     * @return the direction of the blockface.
     */
    public Quaternionf getDirection() {
        return this.direction;
    }

    /**
     * Represents the directional offset of this Blockface as a Vector3i.
     *
     * @return the offset of this directional.
     */
    public Vector3i getOffset() {
        return this.offset;
    }

    /**
     * Gets the opposite BlockFace. If this BlockFace has no opposite the method will return itself.
     *
     * @return the opposite BlockFace, or this if it has no opposite.
     */
    public BlockFace getOpposite() {
        return this.opposite;
    }

    @Override
    public byte getMask() {
        return this.mask;
    }

    /**
     * Uses a yaw angle to get the north, east, west or south face which points into the same direction.
     *
     * @param yaw to use
     * @return the block face
     */
    public static BlockFace fromYaw(float yaw) {
        return BlockFaces.WSEN.get(Math.round(yaw / 90f) & 0x3);
    }

    public static BlockFace fromOffset(Vector3i offset) {
        return OFFSET_MAP.get(getOffsetHash(offset));
    }
}
