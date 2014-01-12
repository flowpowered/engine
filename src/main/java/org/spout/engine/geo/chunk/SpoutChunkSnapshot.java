package org.spout.engine.geo.chunk;

import java.util.List;

import com.flowpowered.commons.datatable.SerializableMap;
import org.spout.api.entity.EntitySnapshot;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.ChunkSnapshot;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFullState;

public class SpoutChunkSnapshot extends ChunkSnapshot {
    private final short[] ids;
    private final short[] data;
    
    public SpoutChunkSnapshot(World world, float x, float y, float z, short[] ids, short[] data) {
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
