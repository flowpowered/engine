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
package com.flowpowered.engine.geo.chunk;

import java.util.List;

import com.flowpowered.commons.datatable.SerializableMap;
import com.flowpowered.api.entity.EntitySnapshot;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.ChunkSnapshot;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.material.block.BlockFullState;

public class FlowChunkSnapshot extends ChunkSnapshot {
    private final short[] ids;
    private final short[] data;
    
    public FlowChunkSnapshot(World world, float x, float y, float z, short[] ids, short[] data) {
        super(world, x, y, z);
        this.ids = ids;
        this.data = data;
    }

    @Override
    public short[] getBlockIds() {
        return ids;
    }

    @Override
    public short[] getBlockData() {
        return data;
    }

    @Override
    public Region getRegion() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<EntitySnapshot> getEntities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPopulated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SerializableMap getDataMap() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<BlockComponentSnapshot> getBlockComponents() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	private int getBlockIndex(int x, int y, int z) {
		return (y & Chunk.BLOCKS.MASK) << Chunk.BLOCKS.DOUBLE_BITS | (z & Chunk.BLOCKS.MASK) << Chunk.BLOCKS.BITS | x & Chunk.BLOCKS.MASK;
	}

	@Override
	public BlockMaterial getBlockMaterial(int x, int y, int z) {
		if (ids == null) {
			throw new UnsupportedOperationException("This chunk snapshot does not contain block ids");
		}
        int index = getBlockIndex(x, y, z);
        return BlockMaterial.get(ids[index], data[index]);
	}

	@Override
	public short getBlockData(int x, int y, int z) {
		if (ids == null) {
			throw new UnsupportedOperationException("This chunk snapshot does not contain block data");
		}
		return BlockFullState.getData(data[this.getBlockIndex(x, y, z)]);
	}

	@Override
	public int getBlockFullState(int x, int y, int z) {
		if (ids == null) {
			throw new UnsupportedOperationException("This chunk snapshot does not contain block data");
		}
        int index = getBlockIndex(x, y, z);
		return BlockFullState.getPacked(ids[index], data[index]);
	}
}
