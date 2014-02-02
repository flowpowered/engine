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

import com.flowpowered.events.Cause;

import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.math.vector.Vector3f;

public interface AreaBlockAccess extends AreaBlockSource {
    /**
     * Sets the data for the block at (x, y, z) to the given data.
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param data to set to
     * @param cause of the change, or null if non-specific cause
     */
    public boolean setBlockData(int x, int y, int z, short data, Cause<?> cause);

    /**
     * Sets the material and data for the block at (x, y, z) to the given material and data.
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param data value to set to
     * @param material to set to
     * @param cause of the change, or null if non-specific cause
     */
    public boolean setBlockMaterial(int x, int y, int z, BlockMaterial material, short data, Cause<?> cause);

    /**
     * Sets the data of the block at (x, y, z) if the expected state matches
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param expect is the state of the block it expects
     * @param data to set to if it matches
     * @param cause of the change, or null if non-specific cause
     * @return whether setting was successful
     */
    public boolean compareAndSetData(int x, int y, int z, int expect, short data, Cause<?> cause);

    /**
     * Sets the given bits in the data for the block at (x, y, z)<br> <br> newData = oldData | (bits)
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param bits the bits to set
     * @return the old data for the block
     */
    public short setBlockDataBits(int x, int y, int z, int bits, Cause<?> cause);

    /**
     * Sets the given bits in the data for the block at (x, y, z)<br> <br> newData = oldData | (bits) <br>or<br> newData = oldData & ~(bits)
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param bits the bits to set or clear
     * @param set true to set, false to clear
     * @return the old data for the block
     */
    public short setBlockDataBits(int x, int y, int z, int bits, boolean set, Cause<?> source);

    /**
     * Clears the given bits in the data for the block at (x, y, z)<br> <br> newData = oldData & (~bits)
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param bits the bits to clear
     * @param cause of the change, or null if non-specific cause
     * @return the old data for the block
     */
    public short clearBlockDataBits(int x, int y, int z, int bits, Cause<?> source);

    /**
     * Gets the data field from the block at (x, y, z)<br> <br> field = (data & bits) >> (shift)<br> <br> The shift value used shifts the least significant non-zero bit of bits to the LSB position
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param bits the bits of the field
     * @return the field value
     */
    public int getBlockDataField(int x, int y, int z, int bits);

    /**
     * Gets if any of the indicated bits are set.
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param bits the bits of the field
     * @return true if any of the given bits are set
     */
    public boolean isBlockDataBitSet(int x, int y, int z, int bits);

    /**
     * Sets the data field for the block at (x, y, z).  This is the reverse operation to the getBlockDataField method.<br> <br> newData = ((value << shift) & bits) | (oldData & (~bits))<br> <br> The
     * shift value used shifts the least significant non-zero bit of bits to the LSB position
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param bits the bits of the field
     * @param value the new value of the field
     * @param cause of the change, or null if non-specific cause
     * @return the old value of the field
     */
    public int setBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source);

    /**
     * Adds a value to the data field for the block at (x, y, z).  This is the reverse operation to the getBlockDataField method.<br> <br> newData = (((oldData + (value << shift)) & bits) | (oldData &
     * ~bits))<br> <br> The shift value used shifts the least significant non-zero bit of bits to the LSB position
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @param bits the bits of the field
     * @param value to add to the value of the field
     * @return the old value of the field
     */
    public int addBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source);

    /**
     * Gets if a block is contained in this area
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @return true if it is contained, false if not
     */
    public boolean containsBlock(int x, int y, int z);

    /**
     * Gets a {@link Block} representing the block at (x, y, z)
     *
     * @param x coordinate of the block
     * @param y coordinate of the block
     * @param z coordinate of the block
     * @return the Block
     */
    public Block getBlock(float x, float y, float z);

    /**
     * Gets a {@link Block} representing the block at the position given
     *
     * @param position of the block
     * @return the Block
     */
    public Block getBlock(Vector3f position);

    /**
     * Atomically sets the cuboid volume to the values inside of the cuboid buffer, if the contents of the buffer's backbuffer matches the world.
     *
     * @param cause that is setting the cuboid volume
     */
    public boolean commitCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause);

    /**
     * Atomically sets the cuboid volume to the values inside of the cuboid buffer.
     *
     * @param cause that is setting the cuboid volume
     */
    public void setCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause);

    /**
     * Atomically sets the cuboid volume to the values inside of the cuboid buffer with the base located at the given coords
     *
     * @param cause that is setting the cuboid volume
     */
    public void setCuboid(int x, int y, int z, CuboidBlockMaterialBuffer buffer, Cause<?> cause);

    /**
     * Atomically gets the cuboid volume
     *
     * @param backBuffer true for a buffer with a back buffer
     */
    public CuboidBlockMaterialBuffer getCuboid(boolean backBuffer);

    /**
     * Atomically gets the cuboid volume with the base located at the given coords of the given size.<br> The buffer returned contains a back buffer <br> Note: The block at the base coordinate is inside
     * the buffer
     *
     * @param bx base x-coordinate
     * @param by base y-coordinate
     * @param bz base z-coordinate
     * @param sx size x-coordinate
     * @param sy size y-coordinate
     * @param sz size z-coordinate
     */
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz);

    /**
     * Atomically gets the cuboid volume with the base located at the given coords of the given size.<br> <br> Note: The block at the base coordinate is inside the buffer
     *
     * @param bx base x-coordinate
     * @param by base y-coordinate
     * @param bz base z-coordinate
     * @param sx size x-coordinate
     * @param sy size y-coordinate
     * @param sz size z-coordinate
     * @param backBuffer true for a buffer with a back buffer
     */
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz, boolean backBuffer);

    /**
     * Atomically gets the cuboid volume with the base located at the given coords and the size of the given buffer.<br> <br> Note: The block at the base coordinate is inside the
     *
     * @param bx base x-coordinate
     * @param by base y-coordinate
     * @param bz base z-coordinate
     */
    public void getCuboid(int bx, int by, int bz, CuboidBlockMaterialBuffer buffer);

    /**
     * Atomically gets the cuboid volume contained within the given buffer
     *
     * @param buffer the buffer
     */
    public void getCuboid(CuboidBlockMaterialBuffer buffer);
}
