package org.spout.engine.geo.region;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.flowpowered.commons.bit.ShortBitMask;
import com.flowpowered.commons.bit.ShortBitSet;
import com.flowpowered.events.Cause;

import org.spout.api.Spout;
import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.ChunkSnapshot;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.io.bytearrayarray.BAAWrapper;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFace;
import org.spout.api.material.block.BlockFaces;
import org.spout.api.scheduler.TaskManager;
import org.spout.api.scheduler.TickStage;
import org.spout.api.util.cuboid.CuboidBlockMaterialBuffer;
import org.spout.engine.SpoutEngine;
import org.spout.engine.entity.EntityManager;
import org.spout.engine.entity.SpoutEntity;
import org.spout.engine.entity.SpoutEntitySnapshot;
import org.spout.engine.filesystem.ChunkDataForRegion;
import org.spout.engine.filesystem.ChunkFiles;
import org.spout.engine.geo.chunk.SpoutChunk;
import org.spout.engine.geo.chunk.SpoutChunkSnapshot;
import org.spout.engine.geo.chunk.SpoutChunkSnapshotGroup;
import org.spout.engine.geo.world.SpoutWorld;
import org.spout.engine.scheduler.RenderThread;
import org.spout.engine.util.thread.AsyncManager;
import org.spout.math.vector.Vector3f;
import org.spout.physics.body.RigidBody;
import org.spout.physics.collision.shape.CollisionShape;

public class SpoutRegion extends Region implements AsyncManager {
	private final RegionGenerator generator;
	/**
	 * Reference to the persistent ByteArrayArray that stores chunk data
	 */
	private final BAAWrapper chunkStore;
    protected final SpoutEngine engine;
	/**
	 * Holds all of the entities to be simulated
	 */
	protected final EntityManager entityManager = new EntityManager();
	// TODO: possibly have a SoftReference of unloaded chunks to allow for quicker loading of chunk
    /**
     * Chunks used for ticking.
     */
	protected final AtomicReference<SpoutChunk[]> chunks = new AtomicReference<>(new SpoutChunk[CHUNKS.VOLUME]);
    /**
     * All live chunks. These are not ticked, but can be accessed.
     */
	protected final AtomicReference<SpoutChunk[]> live = new AtomicReference<>(new SpoutChunk[CHUNKS.VOLUME]);
    protected volatile boolean chunksModified = false;
    private final RenderThread render;

    public SpoutRegion(SpoutEngine engine, World world, float x, float y, float z, BAAWrapper chunkStore, RenderThread render) {
        super(world, x, y, z);
        this.engine = engine;
        this.generator = new RegionGenerator(this, 4);
        this.chunkStore = chunkStore;
        this.render = render;

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

	protected void checkChunkLoaded(SpoutChunk chunk, LoadOption loadopt) {
		if (loadopt.loadIfNeeded()) {
			//if (!chunk.cancelUnload()) {
			//	throw new IllegalStateException("Unloaded chunk returned by getChunk");
			//}
		}
	}

    @Override
    public SpoutChunk getChunk(int x, int y, int z, final LoadOption loadopt) {
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

        final SpoutChunk chunk = chunks.get()[getChunkIndex(x, y, z)];
        if (chunk != null) {
            checkChunkLoaded(chunk, loadopt);
            return chunk;
        }

        if (!loadopt.loadIfNeeded() || !engine.getPlatform().isServer()) {
            return null;
        }

        if (loadopt.isWait()) {
            return loadOrGenChunkImmediately(x, y, z, loadopt);
        }

        final int finalX = x;
        final int finalY = y;
        final int finalZ = z;
        engine.getScheduler().runCoreAsyncTask(new Runnable() {
            @Override
            public void run() {
                loadOrGenChunkImmediately(finalX, finalY, finalZ, loadopt);
            }
        });
        return null;
    }

    // If loadopt.isWait(), this method is run synchronously and so is any further generation
    // If !loadopt.isWait(), this method is run by a runnable, because the loading is taxing; any further generation is also run in its own Runnable
	private SpoutChunk loadOrGenChunkImmediately(int x, int y, int z, final LoadOption loadopt) {
		SpoutChunk newChunk = loadopt.loadIfNeeded() ? loadChunk(x, y, z) : null;

		if (newChunk != null || !loadopt.generateIfNeeded()) {
            return newChunk;
		}

        generator.generateChunk(x, y, z, loadopt.isWait());
        if (!loadopt.isWait()) {
            return null;
        }
        final SpoutChunk generatedChunk = live.get()[getChunkIndex(x, y, z)];
        if (generatedChunk != null) {
            checkChunkLoaded(generatedChunk, loadopt);
            return generatedChunk;
        }
        Spout.getLogger().severe("Chunk failed to generate!  (" + loadopt + ")");
        Spout.getLogger().info("Region " + this + ", chunk " + (getChunkX() + x) + ", " + (getChunkY() + y) + ", " + (getChunkZ() + z));
        Thread.dumpStack();
        return null;
	}

    private SpoutChunk loadChunk(int x, int y, int z) {
        final InputStream stream = this.getChunkInputStream(x, y, z);
        if (stream != null) {
            try {
                try {
                    ChunkDataForRegion dataForRegion = new ChunkDataForRegion();
                    SpoutChunk newChunk = ChunkFiles.loadChunk(this, x, y, z, stream, dataForRegion);
                    if (newChunk == null) {
                        Spout.getLogger().severe("Unable to load chunk at location " + (getChunkX() + x) + ", " + (getChunkY() + y) + ", " + (getChunkZ() + z) + " in region " + this + ", regenerating chunks");
                        return null;
                    }
                    SpoutChunk c = setChunk(newChunk, x, y, z, dataForRegion);
                    checkChunkLoaded(c, LoadOption.LOAD_ONLY);
                    return c;
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                Spout.getLogger().log(Level.WARNING, "IOException when loading chunk!", e);
            }
        }
        return null;
    }

	/**
	 * Gets the DataInputStream corresponding to a given Chunk.<br> <br> The stream is based on a snapshot of the array.
	 *
	 * @param x the chunk
	 * @return the DataInputStream
	 */
	public InputStream getChunkInputStream(int x, int y, int z) {
		return chunkStore.getBlockInputStream(getChunkKey(x, y, z));
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

	protected void setGeneratedChunks(SpoutChunk[][][] newChunks, int baseX, int baseY, int baseZ) {
		while(true) {
            SpoutChunk[] live = this.live.get();
            SpoutChunk[] newArray = Arrays.copyOf(live, live.length);
            final int width = newChunks.length;
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < width; z++) {
                    for (int y = 0; y < width; y++) {
                        int chunkIndex = getChunkIndex(x + baseX, y + baseY, z + baseZ);
                        if (live[chunkIndex] != null) {
                            throw new IllegalStateException("Tried to set a generated chunk, but a chunk already existed!");
                        }
                        newArray[chunkIndex] = newChunks[x][y][z];
                    }
                }
            }
            if (this.live.compareAndSet(live, newArray)) {
                chunksModified = true;
                //newChunk.queueNew();
                break;
			}
        }
	}

	protected SpoutChunk setChunk(SpoutChunk newChunk, int x, int y, int z, ChunkDataForRegion dataForRegion) {
        final int chunkIndex = getChunkIndex(x, y, z);
        while (true) {
            SpoutChunk[] live = this.live.get();
            SpoutChunk old = live[chunkIndex];
            if (old != null) {
                //newChunk.setUnloadedUnchecked();
                return old;
            }
            SpoutChunk[] newArray = Arrays.copyOf(live, live.length);
            newArray[chunkIndex] = newChunk;
            if (this.live.compareAndSet(live, newArray)) {
                chunksModified = true;
                if (dataForRegion != null) {
					for (SpoutEntitySnapshot snapshot : dataForRegion.loadedEntities) {
						SpoutEntity entity = EntityManager.createEntity(snapshot.getTransform());
						entityManager.addEntity(entity);
					}
				}
			}
		}
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

    volatile boolean run = false;
    @Override
    public void copySnapshotRun() {
        entityManager.copyAllSnapshots();
        if (chunksModified) {
            chunks.set(live.get());
            chunksModified = false;
        }
        if (run) return;
        for (SpoutChunk chunk : chunks.get()) {
            if (chunk == null) {
                return;
            }
            run = true;
            ChunkSnapshot[][][] chunks = new ChunkSnapshot[3][3][3];
            int cx = chunk.getX();
            int cy = chunk.getY();
            int cz = chunk.getZ();
            chunks[1][1][1] = new SpoutChunkSnapshot(getWorld(), cx, cy, cz, chunk.getBlockStore().getBlockIdArray(), chunk.getBlockStore().getDataArray());

            for (BlockFace face : BlockFaces.BTNSWE) {
                SpoutChunk local = getWorld().getChunk(getX() + face.getOffset().getFloorX(), getY() + face.getOffset().getFloorY(), getZ() + face.getOffset().getFloorZ(), LoadOption.NO_LOAD);
                if (local == null) {
                    continue;
                }
                chunks[face.getOffset().getFloorX() + 1][face.getOffset().getFloorY() + 1][face.getOffset().getFloorZ() + 1] = new SpoutChunkSnapshot(getWorld(), local.getX(), local.getY(), local.getZ(), local.getBlockStore().getBlockIdArray(), local.getBlockStore().getDataArray());
            }
            render.addChunkModel(render.getMesher().queue(new SpoutChunkSnapshotGroup(cx, cy, cz, chunks)));
        }
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

    protected static int getChunkIndex(int x, int y, int z) {
        return (CHUNKS.AREA * x) + (CHUNKS.SIZE * y) + z;
    }

    @Override
    public SpoutWorld getWorld() {
        return (SpoutWorld) super.getWorld();
    }

    public SpoutChunk[] getChunks() {
        SpoutChunk[] get = chunks.get();
        return Arrays.copyOf(get, get.length);
    }
}
