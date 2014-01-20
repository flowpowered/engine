/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
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
	public Point getPosition();

	/**
	 * Gets the {@link Chunk} this block is in
	 *
	 * @return the Chunk
	 */
	public Chunk getChunk();

	/**
	 * Gets the {@link Region} this block is in
	 *
	 * @return the Region
	 */
	public Region getRegion();

	/**
	 * Gets the {@link World} this block is in
	 *
	 * @return the World
	 */
	@Override
	public World getWorld();

	/**
	 * Gets the x-coordinate of this block
	 *
	 * @return the x-coordinate
	 */
	public int getX();

	/**
	 * Gets the y-coordinate of this block
	 *
	 * @return the y-coordinate
	 */
	public int getY();

	/**
	 * Gets the z-coordinate of this block
	 *
	 * @return the z-coordinate
	 */
	public int getZ();

	/**
	 * Translates this block using the offset and distance given
	 *
	 * @param offset BlockFace to translate
	 * @param distance to translate
	 * @return a new Block instance
	 */
	public Block translate(BlockFace offset, int distance);

	/**
	 * Translates this block using the offset given
	 *
	 * @param offset BlockFace to translate
	 * @return a new Block instance
	 */
	public Block translate(BlockFace offset);

	/**
	 * Translates this block using the offset given
	 *
	 * @param offset Vector to translate
	 * @return a new Block instance
	 */
	public Block translate(Vector3f offset);

	/**
	 * Translates this block using the offset given
	 *
	 * @param offset Vector to translate
	 * @return a new Block instance
	 */
	public Block translate(Vector3i offset);

	/**
	 * Translates this block using the offsets given
	 *
	 * @param dx offset to translate
	 * @param dy offset to translate
	 * @param dz offset to translate
	 * @return a new Block instance
	 */
	public Block translate(int dx, int dy, int dz);

	/**
	 * Gets the block material of this block
	 *
	 * @return block material
	 */
	public BlockMaterial getMaterial();

	/**
	 * Gets the block data for this block
	 *
	 * @return data
	 */
	public short getBlockData();

	/**
	 * Sets the data of this block to the given material's data
	 *
	 * @param data to set to
	 * @return this Block
	 */
	public Block setData(BlockMaterial data);

	/**
	 * Sets the data of this block
	 *
	 * @param data to set to
	 * @return this Block
	 */
	public Block setData(int data);

	/**
	 * Sets the data of this block
	 *
	 * @param data to set to
	 * @param cause of the change
	 * @return this Block
	 */
	public Block setData(int data, Cause<?> cause);

	/**
	 * Adds the value to the data of this block
	 *
	 * @param data to add
	 * @return this Block
	 */
	public Block addData(int data);

	/**
	 * Sets the material of this block
	 *
	 * @param material to set to
	 * @return whether the material set was successful
	 */
	public boolean setMaterial(BlockMaterial material);

	/**
	 * Sets the material of this block
	 *
	 * @param material to set to
	 * @param cause of the change
	 * @return whether the material set was successful
	 */
	public boolean setMaterial(BlockMaterial material, Cause<?> cause);

	/**
	 * Sets the material and data of this block
	 *
	 * @param material to set to
	 * @param data to set to
	 * @return whether the material set was successful
	 */
	public boolean setMaterial(BlockMaterial material, int data);

	/**
	 * Sets the material and data of this block
	 *
	 * @param material to set to
	 * @param data to set to
	 * @param cause of the change
	 * @return whether the material set was successful
	 */
	public boolean setMaterial(BlockMaterial material, int data, Cause<?> cause);

	/**
	 * Sets the given bits in the data for the block<br> <br> newData = oldData | (bits)
	 *
	 * @param bits the bits to set
	 * @return the old data for the block
	 */
	public short setDataBits(int bits);

	/**
	 * Sets the given bits in the data for the block<br> <br> newData = oldData | (bits) <br>or<br> newData = oldData & ~(bits)
	 *
	 * @param bits the bits to set or clear
	 * @param set True to set the bits, False to clear
	 * @return the old data for the block
	 */
	public short setDataBits(int bits, boolean set);

	/**
	 * Clears the given bits in the data for the block<br> <br> newData = oldData & (~bits)
	 *
	 * @param bits the bits to clear
	 * @return the old data for the block
	 */
	public short clearDataBits(int bits);

	/**
	 * Gets the data field from the block<br> <br> field = (data & bits) >> (shift)<br> <br> The shift value used shifts the least significant non-zero bit of bits to the LSB position
	 *
	 * @param bits the bits of the field
	 * @return the field value
	 */
	public int getDataField(int bits);

	/**
	 * Gets if any of the indicated bits are set.
	 *
	 * @param bits the bits to check
	 * @return true if any of the given bits are set
	 */
	public boolean isDataBitSet(int bits);

	/**
	 * Sets the data field for the block.  This is the reverse operation to the getDataField method.<br> <br> newData = ((value << shift) & bits) | (oldData & (~bits))<br> <br> The shift value used
	 * shifts the least significant non-zero bit of bits to the LSB position
	 *
	 * @param bits the bits of the field
	 * @param value the new value of the field
	 * @return the old value of the field
	 */
	public int setDataField(int bits, int value);

	/**
	 * Adds a value to the data field for the block.  This is the reverse operation to the getBlockDataField method.<br> <br> newData = (((oldData + (value << shift)) & bits) | (oldData & ~bits))<br>
	 * <br> The shift value used shifts the least significant non-zero bit of bits to the LSB position
	 *
	 * @param bits the bits of the field
	 * @param value to add to the value of the field
	 * @return the old value of the field
	 */
	public int addDataField(int bits, int value);

	public boolean isMaterial(Material... materials);
}
