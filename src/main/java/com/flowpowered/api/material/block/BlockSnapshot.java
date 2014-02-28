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

import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldManager;
import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.geo.reference.WorldReference;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.material.Material;

import com.flowpowered.commons.hashing.ShortPairHashed;

/**
 * Represents an immutable snapshot of the state of a block
 */
public class BlockSnapshot {
    private final BlockMaterial material;
    private final short data;
    private final int x, y, z;
    private final WorldReference world;

    public BlockSnapshot(Block block) {
        this(block, block.getMaterial(), block.getBlockData());
    }

    public BlockSnapshot(Block block, BlockMaterial material, short data) {
        this(block.getWorld(), block.getX(), block.getY(), block.getZ(), material, data);
    }

    public BlockSnapshot(WorldReference world, int x, int y, int z, BlockMaterial material, short data) {
        this.material = material;
        this.data = data;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    /**
     * Gets the x-coordinate of this Block snapshot
     *
     * @return the x-coordinate
     */
    public int getX() {
        return this.x;
    }

    /**
     * Gets the y-coordinate of this Block snapshot
     *
     * @return the y-coordinate
     */
    public int getY() {
        return this.y;
    }

    /**
     * Gets the z-coordinate of this Block snapshot
     *
     * @return the z-coordinate
     */
    public int getZ() {
        return this.z;
    }

    /**
     * Gets the world this Block snapshot is in
     *
     * @return the World
     */
    public WorldReference getWorld() {
        return this.world;
    }

    /**
     * Gets which block corresponding to the snapshot
     *
     * @return the block
     */
    public Block getBlock(WorldManager manager) {
        return this.world.refresh(manager).getBlock(this.x, this.y, this.z);
    }

    /**
     * Gets the block's material at the time of the snapshot
     *
     * @return the material
     */
    public BlockMaterial getMaterial() {
        return this.material;
    }

    public short getData() {
        return this.data;
    }

    @Override
    public String toString() {
        return "BlockSnapshot { material: " + this.material + " , data:" + this.data + ", x: " + x + ", y: " + y + ", z: " + z + "}";
    }

    @Override
    public int hashCode() {
        return ShortPairHashed.key(this.material.getId(), this.data);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BlockSnapshot) {
            BlockSnapshot other = (BlockSnapshot) o;
            return other.x == x && other.y == y && other.z == z && other.material.equals(material) && other.data == data;
        }
        return false;
    }

    public boolean isMaterial(Material... materials) {
        if (this.material == null) {
            for (Material material : materials) {
                if (material == null) {
                    return true;
                }
            }
            return false;
        } else {
            return this.material.isMaterial(materials);
        }
    }
}
