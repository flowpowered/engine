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
package com.flowpowered.engine.geo.region;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Level;

import com.flowpowered.commons.bit.ShortBitMask;
import com.flowpowered.commons.bit.ShortBitSet;
import com.flowpowered.events.Cause;
import com.flowpowered.math.vector.Vector3f;

import com.flowpowered.api.Flow;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.io.bytearrayarray.BAAWrapper;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.material.block.BlockFace;
import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.entity.EntityManager;
import com.flowpowered.engine.entity.FlowEntity;
import com.flowpowered.engine.entity.FlowEntitySnapshot;
import com.flowpowered.engine.filesystem.ChunkDataForRegion;
import com.flowpowered.engine.filesystem.ChunkFiles;
import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.geo.snapshot.RegionSnapshot;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.scheduler.render.RenderThread;
import com.flowpowered.engine.util.thread.CompleteAsyncManager;
import org.spout.physics.body.RigidBody;
import org.spout.physics.collision.shape.CollisionShape;

public class FlowRegion extends Region implements CompleteAsyncManager {
    private final RegionGenerator generator;
    /**
     * Reference to the persistent ByteArrayArray that stores chunk data
     */
    private final BAAWrapper chunkStore;
    protected final FlowEngine engine;
    /**
     * Holds all of the entities to be simulated
     */
    protected final EntityManager entityManager = new EntityManager();
    // TODO: possibly have a SoftReference of unloaded chunks to allow for quicker loading of chunk
    /**
     * Chunks used for ticking.
     */
    protected final AtomicReference<FlowChunk[]> chunks = new AtomicReference<>(new FlowChunk[CHUNKS.VOLUME]);
    /**
     * All live chunks. These are not ticked, but can be accessed.
     */
    protected final AtomicReference<FlowChunk[]> live = new AtomicReference<>(new FlowChunk[CHUNKS.VOLUME]);
    private final RegionSnapshot snapshot;
    private final RenderThread render;

    public FlowRegion(FlowEngine engine, FlowWorld world, int x, int y, int z, BAAWrapper chunkStore, RenderThread render) {
        super(world, x << BLOCKS.BITS, y << BLOCKS.BITS, z << BLOCKS.BITS);
        this.engine = engine;
        this.generator = new RegionGenerator(this, 4);
        this.chunkStore = chunkStore;
        this.render = render;
        this.snapshot = new RegionSnapshot(world.getSnapshot(), getPosition().toInt());

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
    public boolean isLoaded() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void checkChunkLoaded(FlowChunk chunk, LoadOption loadopt) {
        if (loadopt.loadIfNeeded()) {
            //if (!chunk.cancelUnload()) {
            //    throw new IllegalStateException("Unloaded chunk returned by getChunk");
            //}
        }
    }

    @Override
    public FlowChunk getChunk(final int x, final int y, final int z, final LoadOption loadopt) {
        // If we're not waiting, then we don't care because it's async anyways
        if (loadopt.isWait()) {
            if (loadopt.generateIfNeeded()) {
                TickStage.checkStage(TickStage.noneOf(TickStage.SNAPSHOT, TickStage.PRESNAPSHOT, TickStage.LIGHTING));
            } else if (loadopt.loadIfNeeded()) {
                TickStage.checkStage(TickStage.noneOf(TickStage.SNAPSHOT));
            }
        }

        final int localX = x & CHUNKS.MASK;
        final int localY = y & CHUNKS.MASK;
        final int localZ = z & CHUNKS.MASK;

        final FlowChunk chunk = chunks.get()[getChunkIndex(localX, localY, localZ)];
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

        engine.getScheduler().runCoreAsyncTask(new Runnable() {
            @Override
            public void run() {
                loadOrGenChunkImmediately(x, y, z, loadopt);
            }
        });
        return null;
    }

    // If loadopt.isWait(), this method is run synchronously and so is any further generation
    // If !loadopt.isWait(), this method is run by a runnable, because the loading is taxing; any further generation is also run in its own Runnable
    private FlowChunk loadOrGenChunkImmediately(int worldX, int worldY, int worldZ, final LoadOption loadopt) {
        final int localX = worldX & CHUNKS.MASK;
        final int localY = worldY & CHUNKS.MASK;
        final int localZ = worldZ & CHUNKS.MASK;
        FlowChunk newChunk = loadopt.loadIfNeeded() ? loadChunk(localX, localY, localZ) : null;

        if (newChunk != null || !loadopt.generateIfNeeded()) {
            return newChunk;
        }

        generator.generateChunk(worldX, worldY, worldZ, loadopt.isWait());
        if (!loadopt.isWait()) {
            return null;
        }
        final FlowChunk generatedChunk = live.get()[getChunkIndex(localX, localY, localZ)];
        if (generatedChunk != null) {
            checkChunkLoaded(generatedChunk, loadopt);
            return generatedChunk;
        }
        engine.getLogger().error("Chunk failed to generate!  (" + loadopt + ")");
        engine.getLogger().info("Region " + this + ", chunk " + worldX + ", " + worldY + ", " + worldZ);
        Thread.dumpStack();
        return null;
    }

    private FlowChunk loadChunk(int x, int y, int z) {
        final InputStream stream = this.getChunkInputStream(x, y, z);
        if (stream != null) {
            try {
                try {
                    ChunkDataForRegion dataForRegion = new ChunkDataForRegion();
                    FlowChunk newChunk = ChunkFiles.loadChunk(this, x, y, z, stream, dataForRegion);
                    if (newChunk == null) {
                        engine.getLogger().error("Unable to load chunk at location " + (getChunkX() + x) + ", " + (getChunkY() + y) + ", " + (getChunkZ() + z) + " in region " + this + ", regenerating chunks");
                        return null;
                    }
                    FlowChunk c = setChunk(newChunk, x, y, z, dataForRegion);
                    checkChunkLoaded(c, LoadOption.LOAD_ONLY);
                    return c;
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                engine.getLogger().log(Level.WARN, "IOException when loading chunk!", e);
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

    protected void setGeneratedChunks(FlowChunk[][][] newChunks, int baseX, int baseY, int baseZ) {
        while(true) {
            FlowChunk[] live = this.live.get();
            FlowChunk[] newArray = Arrays.copyOf(live, live.length);
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
                //newChunk.queueNew();
                break;
            }
        }
    }

    protected FlowChunk setChunk(FlowChunk newChunk, int x, int y, int z, ChunkDataForRegion dataForRegion) {
        final int chunkIndex = getChunkIndex(x, y, z);
        while (true) {
            FlowChunk[] live = this.live.get();
            FlowChunk old = live[chunkIndex];
            if (old != null) {
                //newChunk.setUnloadedUnchecked();
                return old;
            }
            FlowChunk[] newArray = Arrays.copyOf(live, live.length);
            newArray[chunkIndex] = newChunk;
            if (this.live.compareAndSet(live, newArray)) {
                if (dataForRegion != null) {
                    for (FlowEntitySnapshot snapshot : dataForRegion.loadedEntities) {
                        FlowEntity entity = EntityManager.createEntity(engine, snapshot.getTransform());
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
    public void copySnapshotRun(int sequence) {
        entityManager.copyAllSnapshots();
        chunks.set(live.get());
        snapshot.update(this);
    }

    @Override
    public void startTickRun(int stage, long delta) {
        if (stage == 0) {
            updateEntities(delta);
        }
    }


	private void updateEntities(float dt) {
		for (FlowEntity ent : entityManager.getAll()) {
			try {
				ent.tick(dt);
			} catch (Exception e) {
				engine.getLogger().log(Level.ERROR, "Unhandled exception during tick for " + ent.toString(), e);
			}
		}
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
    public boolean checkSequence(TickStage stage, int seqence) {
        if (stage == TickStage.SNAPSHOT) {
            return seqence == -1;
        }
        return seqence == -1;
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
    public FlowWorld getWorld() {
        return (FlowWorld) super.getWorld();
    }

    public FlowChunk[] getChunks() {
        FlowChunk[] get = chunks.get();
        return Arrays.copyOf(get, get.length);
    }

    public RegionSnapshot getSnapshot() {
        return snapshot;
    }
}
