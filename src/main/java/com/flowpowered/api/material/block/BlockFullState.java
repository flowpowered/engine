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

import com.flowpowered.commons.StringUtil;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.flowpowered.api.material.BlockMaterial;

/**
 * Represents a {@link Block}'s ID and Data values, but contains no location-specific information.
 */
public class BlockFullState implements Cloneable {
    private final short id;
    private final short data;

    public BlockFullState(int idAndData) {
        id = (short) (idAndData >> 16);
        data = (short) (idAndData);
    }

    public BlockFullState(short id, short data) {
        this.id = id;
        this.data = data;
    }

    /**
     * Id of the Block
     *
     * @return id
     */
    public final short getId() {
        return id;
    }

    /**
     * Data value of the Block
     *
     * @return data
     */
    public final short getData() {
        return data;
    }

    /**
     * Returns an Integer representation of the merged ID and data for this BlockFullState.<br/> The id will be contained in the upper 16-bits. The data will be contained in the lower 16-bits.<br/>
     *
     * @return integer representation of ID and Data.
     */
    public int getPacked() {
        return getPacked(id, data);
    }

    /**
     * Returns an Integer representation of the merged ID and data.<br/> The id will be contained in the upper 16-bits. The data will be contained in the lower 16-bits.<br/>
     *
     * @param id to pack.
     * @param data to pack.
     * @return integer representation of ID and Data.
     */
    public static int getPacked(short id, short data) {
        return id << 16 | (data & 0xFFFF);
    }

    /**
     * Returns an Integer representation of the ID and Data from a {@link BlockMaterial}.<br/> The id will be contained in the upper 16-bits. The data will be contained in the lower 16-bits.<br/>
     */
    public static int getPacked(BlockMaterial m) {
        return getPacked(m.getId(), m.getData());
    }

    /**
     * Unpacks the ID of a Material or Block from a packed integer.<br/> The integer being passed in must have the ID of the Material or Block contained in the upper 16-bits.<br/>
     *
     * @param packed integer
     * @return id of the material or block
     */
    public static short getId(int packed) {
        return (short) (packed >> 16);
    }

    /**
     * Unpacks the Data of a material or block from a packed integer.<br/> The integer being passed in must have the data of the Material or Block contained in the lower 16-bits.<br/>
     *
     * @param packed integer
     * @return data of the material or block.
     */
    public static short getData(int packed) {
        return (short) packed;
    }

    /**
     * Looks up the BlockMaterial from a packed integer.<br/> If the material does not exist in the {@link BlockMaterialRegistry} then {@link BasicAir} will be returned. If the material does exist, and
     * it contains data, the Sub-Material will be returned.
     *
     * @return the material found.
     */
    public static BlockMaterial getMaterial(int packed) {
        short id = getId(packed);
        short data = getData(packed);
        BlockMaterial mat = BlockMaterial.get(id);
        if (mat == null) {
            return BlockMaterial.AIR;
        }
        return mat.getSubMaterial(data);
    }

    @Override
    public String toString() {
        return StringUtil.toNamedString(this, this.id, this.data);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(77, 81).append(id).append(data).toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BlockFullState)) {
            return false;
        } else {
            BlockFullState fullState = (BlockFullState) o;

            return fullState.id == id && fullState.data == data;
        }
    }

    @Override
    public BlockFullState clone() {
        return new BlockFullState(id, data);
    }
}
