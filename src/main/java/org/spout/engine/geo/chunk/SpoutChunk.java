package org.spout.engine.geo.chunk;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.flowpowered.commons.datatable.ManagedHashMap;
import com.flowpowered.commons.store.block.AtomicBlockStore;
import com.flowpowered.events.Cause;

import gnu.trove.map.hash.TShortObjectHashMap;

import org.spout.api.component.BlockComponentOwner;
import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.BlockComponentContainer;
import org.spout.api.geo.cuboid.BlockContainer;
import org.spout.api.geo.cuboid.Chunk;
import static org.spout.api.geo.cuboid.Chunk.BLOCKS;
import org.spout.api.geo.cuboid.ChunkSnapshot;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFullState;
import org.spout.api.util.cuboid.CuboidBlockMaterialBuffer;
import org.spout.api.util.hashing.NibbleQuadHashed;
import org.spout.engine.geo.SpoutBlock;
import org.spout.engine.geo.region.SpoutRegion;
import org.spout.engine.geo.world.SpoutWorld;
import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3f;

public class SpoutChunk extends Chunk {

    private final SpoutRegion region;
	/**
	 * Data map and Datatable associated with it
	 */
	protected final ManagedHashMap dataMap;
    /**
	 * Not thread safe, synchronize on access
	 */
	private final TShortObjectHashMap<BlockComponentOwner> blockComponents = new TShortObjectHashMap<>();
    private final int generationIndex;
	/**
	 * Storage for block ids, data and auxiliary data. For blocks with data = 0 and auxiliary data = null, the block is stored as a short.
	 */
	protected final AtomicBlockStore blockStore;

    public SpoutChunk(SpoutRegion region, World world, int x, int y, int z, int generationIndex, AtomicBlockStore blockStore) {
        super(world, x, y, z);
        this.region = region;
        this.dataMap = new ManagedHashMap();
        this.generationIndex = generationIndex;
        this.blockStore = blockStore;
    }

    @Override
    public void unload(boolean save) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChunkSnapshot getSnapshot() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChunkSnapshot getSnapshot(ChunkSnapshot.SnapshotType type, ChunkSnapshot.EntityType entities, ChunkSnapshot.ExtraData data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillBlockContainer(BlockContainer container) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillBlockComponentContainer(BlockComponentContainer container) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Future<ChunkSnapshot> getFutureSnapshot() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Future<ChunkSnapshot> getFutureSnapshot(ChunkSnapshot.SnapshotType type, ChunkSnapshot.EntityType entities, ChunkSnapshot.ExtraData data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean refreshObserver(Entity player) {
        return true;
    }

    @Override
    public boolean removeObserver(Entity player) {
        return true;
    }

    @Override
    public SpoutRegion getRegion() {
        return region;
    }

    @Override
    public boolean isLoaded() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean populate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean populate(boolean force) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void populate(boolean sync, boolean observe) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void populate(boolean sync, boolean observe, boolean priority) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPopulated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getEntities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getLiveEntities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNumObservers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<? extends Player> getObservingPlayers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<? extends Entity> getObservers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getGenerationIndex() {
        return generationIndex;
    }

    @Override
    public boolean setBlockData(int x, int y, int z, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addBlockData(int x, int y, int z, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean setBlockMaterial(int x, int y, int z, BlockMaterial material, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean compareAndSetData(int x, int y, int z, int expect, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short setBlockDataBits(int x, int y, int z, int bits, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short setBlockDataBits(int x, int y, int z, int bits, boolean set, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short clearBlockDataBits(int x, int y, int z, int bits, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBlockDataField(int x, int y, int z, int bits) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isBlockDataBitSet(int x, int y, int z, int bits) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int setBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int addBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	@Override
	public SpoutBlock getBlock(int x, int y, int z) {
		return new SpoutBlock((SpoutWorld) getWorld(), x, y, z);
	}

	@Override
	public SpoutBlock getBlock(float x, float y, float z) {
		return this.getBlock(GenericMath.floor(x), GenericMath.floor(y), GenericMath.floor(z));
	}

	@Override
	public SpoutBlock getBlock(Vector3f position) {
		return this.getBlock(position.getX(), position.getY(), position.getZ());
	}

    @Override
    public boolean commitCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCuboid(int x, int y, int z, CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(boolean backBuffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz, boolean backBuffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCuboid(int bx, int by, int bz, CuboidBlockMaterialBuffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCuboid(CuboidBlockMaterialBuffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
	public BlockMaterial getBlockMaterial(int x, int y, int z) {
		int state = blockStore.getFullData(x & BLOCKS.MASK, y & BLOCKS.MASK, z & BLOCKS.MASK);
		return BlockMaterial.get(state);
    }

    @Override
    public int getBlockFullState(int x, int y, int z) {
        return blockStore.getFullData(x & BLOCKS.MASK, y & BLOCKS.MASK, z & BLOCKS.MASK);
    }

    @Override
    public short getBlockData(int x, int y, int z) {
        return blockStore.getData(x & BLOCKS.MASK, y & BLOCKS.MASK, z & BLOCKS.MASK);
    }

    public AtomicBlockStore getBlockStore() {
        return blockStore;
    }

	public BlockComponentOwner getBlockComponentOwner(int x, int y, int z, boolean create) {
		synchronized (blockComponents) {
			short packed = NibbleQuadHashed.key(x, y, z, 0);
			BlockComponentOwner value = blockComponents.get(packed);
			if (value == null && create) {
				value = new BlockComponentOwner(dataMap, NibbleQuadHashed.key1(packed), NibbleQuadHashed.key2(packed), NibbleQuadHashed.key3(packed), getWorld());
				blockComponents.put(packed, value);
			}
			return value;
		}
	}
}
