package org.spout.engine.geo;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.commons.bit.ShortBitMask;
import com.flowpowered.commons.bit.ShortBitSet;
import com.flowpowered.events.Cause;

import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.Region;
import static org.spout.api.geo.cuboid.Region.CHUNKS;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFace;
import org.spout.api.scheduler.TaskManager;
import org.spout.api.scheduler.TickStage;
import org.spout.api.util.cuboid.CuboidBlockMaterialBuffer;
import org.spout.engine.SpoutEngine;
import org.spout.engine.entity.EntityManager;
import org.spout.engine.util.thread.AsyncManager;
import org.spout.math.vector.Vector3f;
import org.spout.physics.body.RigidBody;
import org.spout.physics.collision.shape.CollisionShape;

public class SpoutRegion extends Region implements AsyncManager {
    protected final SpoutEngine engine;
	/**
	 * Holds all of the entities to be simulated
	 */
	protected final EntityManager entityManager = new EntityManager();
	@SuppressWarnings ("unchecked")
	// TODO: possibly have a SoftReference of unloaded chunks to allow for quicker loading of chunk
	protected final AtomicReference<SpoutChunk>[][][] chunks = new AtomicReference[CHUNKS.SIZE][CHUNKS.SIZE][CHUNKS.SIZE];
	protected final AtomicReference<SpoutChunk>[][][] live = new AtomicReference[CHUNKS.SIZE][CHUNKS.SIZE][CHUNKS.SIZE];
    protected final boolean chunksModified = false;

    public SpoutRegion(SpoutEngine engine, World world, float x, float y, float z) {
        super(world, x, y, z);
        this.engine = engine;
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unload(boolean save) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity getEntity(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Player> getPlayers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TaskManager getTaskManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isLoaded() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	private void checkChunkLoaded(SpoutChunk chunk, LoadOption loadopt) {
		if (loadopt.loadIfNeeded()) {
			//if (!chunk.cancelUnload()) {
			//	throw new IllegalStateException("Unloaded chunk returned by getChunk");
			//}
		}
	}

    @Override
    public Chunk getChunk(int x, int y, int z, final LoadOption loadopt) {
        // If we're not waiting, then we don't care because it's async anyways
        if (loadopt.isWait()) {
            if (loadopt.generateIfNeeded()) {
                TickStage.checkStage(TickStage.noneOf(TickStage.SNAPSHOT, TickStage.PRESNAPSHOT, TickStage.LIGHTING));
            } else if (loadopt.loadIfNeeded()) {
                TickStage.checkStage(TickStage.noneOf(TickStage.SNAPSHOT));
            }
        }

        x &= CHUNKS.MASK;
        y &= CHUNKS.MASK;
        z &= CHUNKS.MASK;

        final SpoutChunk chunk = chunks[x][y][z].get();
        if (chunk != null) {
            checkChunkLoaded(chunk, loadopt);
        }
        return chunk;
    }

	public static int getChunkKey(int chunkX, int chunkY, int chunkZ) {
		chunkX &= CHUNKS.MASK;
		chunkY &= CHUNKS.MASK;
		chunkZ &= CHUNKS.MASK;

		int key = 0;
		key |= chunkX;
		key |= chunkY << CHUNKS.BITS;
		key |= chunkZ << (CHUNKS.BITS << 1);

		return key;
	}

    @Override
    public Chunk getChunkFromBlock(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getChunkFromBlock(Vector3f position, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveChunk(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unloadChunk(int x, int y, int z, boolean save) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNumLoadedChunks() {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public Block getBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Block getBlock(float x, float y, float z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Block getBlock(Vector3f position) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBlockFullState(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short getBlockData(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getLocalRegion(BlockFace face, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getLocalRegion(int dx, int dy, int dz, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(Chunk c, BlockFace face, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(Chunk c, int ox, int oy, int oz, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(int x, int y, int z, int ox, int oy, int oz, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public void finalizeRun() {
        entityManager.finalizeRun();
    }

    @Override
    public void preSnapshotRun() {
        entityManager.preSnapshotRun();
    }

    @Override
    public void copySnapshotRun() {
        entityManager.copyAllSnapshots();
    }

    @Override
    public void startTickRun(int stage, long delta) {
    }

    @Override
    public void runPhysics(int sequence) {
    }

    @Override
    public void runDynamicUpdates(long threshold, int sequence) {
    }

    @Override
    public void runLighting(int sequence) {
    }

    @Override
    public int getSequence() {
        return -1;
    }

    @Override
    public long getFirstDynamicUpdateTime() {
        return 0;
    }

    @Override
    public Thread getExecutionThread() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExecutionThread(Thread t) {
    }

    private static ShortBitSet ALL_STAGES = new ShortBitSet(Short.MAX_VALUE);
    @Override
    public ShortBitMask getTickStages() {
        return ALL_STAGES;
    }

    public void removeBody(RigidBody body) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RigidBody addBody(Transform live, float mass, CollisionShape shape, boolean ghost, boolean mobile) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
