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
package com.flowpowered.api.geo.cuboid;

import com.flowpowered.events.Cause;

import com.flowpowered.api.component.ComponentOwner;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldSource;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.material.Material;
import com.flowpowered.api.material.block.BlockFace;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;

public interface Block extends WorldSource, ComponentOwner {
    /**
     * Gets the {@link Point} position of this block in the world
     *
     * @return the position
     */
    Point getPosition();

    /**
     * Gets the {@link Chunk} this block is in
     *
     * @return the Chunk
     */
    Chunk getChunk();

    /**
     * Gets the {@link Region} this block is in
     *
     * @return the Region
     */
    Region getRegion();

    /**
     * Gets the {@link World} this block is in
     *
     * @return the World
     */
    @Override
    World getWorld();

    /**
     * Gets the x-coordinate of this block
     *
     * @return the x-coordinate
     */
    int getX();

    /**
     * Gets the y-coordinate of this block
     *
     * @return the y-coordinate
     */
    int getY();

    /**
     * Gets the z-coordinate of this block
     *
     * @return the z-coordinate
     */
    int getZ();

    /**
     * Translates this block using the offset and distance given
     *
     * @param offset BlockFace to translate
     * @param distance to translate
     * @return a new Block instance
     */
    Block translate(BlockFace offset, int distance);

    /**
     * Translates this block using the offset given
     *
     * @param offset BlockFace to translate
     * @return a new Block instance
     */
    Block translate(BlockFace offset);

    /**
     * Translates this block using the offset given
     *
     * @param offset Vector to translate
     * @return a new Block instance
     */
    Block translate(Vector3f offset);

    /**
     * Translates this block using the offset given
     *
     * @param offset Vector to translate
     * @return a new Block instance
     */
    Block translate(Vector3i offset);

    /**
     * Translates this block using the offsets given
     *
     * @param dx offset to translate
     * @param dy offset to translate
     * @param dz offset to translate
     * @return a new Block instance
     */
    Block translate(int dx, int dy, int dz);

    /**
     * Gets the block material of this block
     *
     * @return block material
     */
    BlockMaterial getMaterial();

    /**
     * Gets the block data for this block
     *
     * @return data
     */
    short getBlockData();

    /**
     * Sets the data of this block to the given material's data
     *
     * @param data to set to
     * @return this Block
     */
    Block setData(BlockMaterial data);

    /**
     * Sets the data of this block
     *
     * @param data to set to
     * @return this Block
     */
    Block setData(int data);

    /**
     * Sets the data of this block
     *
     * @param data to set to
     * @param cause of the change
     * @return this Block
     */
    Block setData(int data, Cause<?> cause);

    /**
     * Sets the material of this block
     *
     * @param material to set to
     * @return whether the material set was successful
     */
    boolean setMaterial(BlockMaterial material);

    /**
     * Sets the material of this block
     *
     * @param material to set to
     * @param cause of the change
     * @return whether the material set was successful
     */
    boolean setMaterial(BlockMaterial material, Cause<?> cause);

    /**
     * Sets the material and data of this block
     *
     * @param material to set to
     * @param data to set to
     * @return whether the material set was successful
     */
    boolean setMaterial(BlockMaterial material, int data);

    /**
     * Sets the material and data of this block
     *
     * @param material to set to
     * @param data to set to
     * @param cause of the change
     * @return whether the material set was successful
     */
    boolean setMaterial(BlockMaterial material, int data, Cause<?> cause);

    /**
     * Sets the given bits in the data for the block<br> <br> newData = oldData | (bits)
     *
     * @param bits the bits to set
     * @return the old data for the block
     */
    short setDataBits(int bits);

    /**
     * Sets the given bits in the data for the block<br> <br> newData = oldData | (bits) <br>or<br> newData = oldData & ~(bits)
     *
     * @param bits the bits to set or clear
     * @param set True to set the bits, False to clear
     * @return the old data for the block
     */
    short setDataBits(int bits, boolean set);

    /**
     * Clears the given bits in the data for the block<br> <br> newData = oldData & (~bits)
     *
     * @param bits the bits to clear
     * @return the old data for the block
     */
    short clearDataBits(int bits);

    /**
     * Gets the data field from the block<br> <br> field = (data & bits) >> (shift)<br> <br> The shift value used shifts the least significant non-zero bit of bits to the LSB position
     *
     * @param bits the bits of the field
     * @return the field value
     */
    int getDataField(int bits);

    /**
     * Gets if any of the indicated bits are set.
     *
     * @param bits the bits to check
     * @return true if any of the given bits are set
     */
    boolean isDataBitSet(int bits);

    /**
     * Sets the data field for the block.  This is the reverse operation to the getDataField method.<br> <br> newData = ((value << shift) & bits) | (oldData & (~bits))<br> <br> The shift value used
     * shifts the least significant non-zero bit of bits to the LSB position
     *
     * @param bits the bits of the field
     * @param value the new value of the field
     * @return the old value of the field
     */
    int setDataField(int bits, int value);

    /**
     * Adds a value to the data field for the block.  This is the reverse operation to the getBlockDataField method.<br> <br> newData = (((oldData + (value << shift)) & bits) | (oldData & ~bits))<br>
     * <br> The shift value used shifts the least significant non-zero bit of bits to the LSB position
     *
     * @param bits the bits of the field
     * @param value to add to the value of the field
     * @return the old value of the field
     */
    int addDataField(int bits, int value);

    boolean isMaterial(Material... materials);
}
