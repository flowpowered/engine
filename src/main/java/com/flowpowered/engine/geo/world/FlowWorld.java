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
package com.flowpowered.engine.geo.world;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.flowpowered.commons.bit.ShortBitMask;
import com.flowpowered.events.Cause;

import com.flowpowered.api.component.BaseComponentOwner;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.EntityPrefab;
import com.flowpowered.api.player.Player;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.scheduler.TaskManager;
import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.entity.EntityManager;
import com.flowpowered.engine.entity.FlowEntity;
import com.flowpowered.engine.geo.region.RegionSource;
import com.flowpowered.engine.geo.FlowBlock;
import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.geo.region.FlowRegion;
import com.flowpowered.engine.geo.snapshot.FlowWorldSnapshot;
import com.flowpowered.engine.util.thread.CopySnapshotManager;
import com.flowpowered.engine.util.thread.StartTickManager;
import com.flowpowered.engine.util.thread.snapshotable.SnapshotManager;
import com.flowpowered.engine.util.thread.snapshotable.SnapshotableLong;
import com.flowpowered.math.GenericMath;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;

public class FlowWorld extends BaseComponentOwner implements World, StartTickManager, CopySnapshotManager {
    // TEST CODE
    /**
     * Number of milliseconds in a day.
     */
    public static final long MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
    /**
     * The duration of a day in the game, in real life, in milliseconds.
     */
    public static final long GAME_DAY_IRL = 1000 * 60;
    private final FlowEngine engine;
    private final String name;
    private final UUID uid;
    private final SnapshotManager snapshotManager;
    private final SnapshotableLong age;
    private final RegionSource regionSource;
    private final FlowWorldSnapshot snapshot;

    public FlowWorld(FlowEngine engine, String name, UUID uid, long age) {
        super(engine);
        this.engine = engine;
        this.name = name;
        this.uid = uid;
        this.snapshotManager = new SnapshotManager();
        this.age = new SnapshotableLong(snapshotManager, age);
        this.regionSource = new RegionSource(engine, (FlowServerWorld) this);
        this.snapshot = new FlowWorldSnapshot(this);
    }

    public FlowWorld(FlowEngine engine, String name) {
        this(engine, name, UUID.randomUUID(), 0);
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getAge() {
        return age.get();
    }

    @Override
    public UUID getUID() {
        return uid;
    }

    @Override
    public Entity getEntity(UUID uid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity spawnEntity(Vector3f point, LoadOption option, EntityPrefab prefab) {
        return spawnEntity(point, option, prefab.getComponents().toArray(new Class[0]));
    }

    @Override
    public Entity spawnEntity(Vector3f point, LoadOption option, Class<? extends Component>... classes) {
        FlowRegion region = getRegionFromBlock(point, option);
        if (region == null) {
            return null;
        }

        FlowEntity entity = EntityManager.createEntity(getEngine(), new Transform(new Point(this, point), Quaternionf.fromAxesAnglesDeg(0, 0, 0), Vector3f.ONE));
        region.getEntityManager().addEntity(entity);
        return entity;
    }

    @Override
    public Entity[] spawnEntities(Vector3f[] points, LoadOption option, Class<? extends Component>... classes) {
        Entity[] entities = new Entity[points.length];
        for (int i = 0; i < points.length; i++) {
            entities[i] = spawnEntity(points[i], option, classes);
        }
        return entities;
    }

    @Override
    public FlowEngine getEngine() {
        return engine;
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
    public TaskManager getTaskManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getNearbyEntities(Point position, Entity ignore, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getNearbyEntities(Point position, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getNearbyEntities(Entity entity, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity getNearestEntity(Point position, Entity ignore, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity getNearestEntity(Point position, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity getNearestEntity(Entity entity, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Player> getNearbyPlayers(Point position, Player ignore, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Player> getNearbyPlayers(Point position, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Player> getNearbyPlayers(Entity entity, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Player getNearestPlayer(Point position, Player ignore, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Player getNearestPlayer(Point position, int range) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Player getNearestPlayer(Entity entity, int range) {
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
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz) {
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
    public List<Player> getPlayers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Region> getRegions() {
        return regionSource.getRegions();
    }

    public Collection<FlowRegion> getFlowRegions() {
        return (Collection) getRegions();
    }

    @Override
    public FlowRegion getRegion(int x, int y, int z, LoadOption loadopt) {
        return regionSource.getRegion(x, y, z, loadopt);
    }

    @Override
    public FlowRegion getRegionFromChunk(int x, int y, int z, LoadOption loadopt) {
        return getRegion(x >> Region.CHUNKS.BITS, y >> Region.CHUNKS.BITS, z >> Region.CHUNKS.BITS, loadopt);
    }

    @Override
    public FlowRegion getRegionFromBlock(int x, int y, int z, LoadOption loadopt) {
        return getRegion(x >> Region.BLOCKS.BITS, y >> Region.BLOCKS.BITS, z >> Region.BLOCKS.BITS, loadopt);
    }

    @Override
    public FlowRegion getRegionFromBlock(Vector3f position, LoadOption loadopt) {
        return getRegionFromBlock(position.getFloorX(), position.getFloorY(), position.getFloorZ(), loadopt);
    }

    @Override
    public boolean containsChunk(int x, int y, int z) {
        return true;
    }

    @Override
    public FlowChunk getChunk(int x, int y, int z, LoadOption loadopt) {
        FlowRegion region = getRegionFromChunk(x, y, z, loadopt);
        if (region == null) {
            return null;
        }
        return region.getChunk(x, y, z, loadopt);
    }

    @Override
    public Chunk getChunkFromBlock(int x, int y, int z, LoadOption loadopt) {
        Region region = getRegionFromBlock(x, y, z, loadopt);
        if (region == null) {
            return null;
        }
        return region.getChunkFromBlock(x, y, z, loadopt);
    }

    @Override
    public Chunk getChunkFromBlock(Vector3f position, LoadOption loadopt) {
        Region region = getRegionFromBlock(position, loadopt);
        if (region == null) {
            return null;
        }
        return region.getChunkFromBlock(position, loadopt);
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
    public boolean containsBlock(int x, int y, int z) {
        return true;
    }

    @Override
    public FlowBlock getBlock(float x, float y, float z) {
        return new FlowBlock(this, GenericMath.floor(x), GenericMath.floor(y), GenericMath.floor(z));
    }

    @Override
    public FlowBlock getBlock(Vector3f position) {
        return this.getBlock(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean commitCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(boolean backBuffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz, boolean backBuffer) {
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

    public FlowWorldSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public void startTickRun(int stage, long delta) {
        if (stage == 0) {
            age.set((long) (age.get() + (delta / 1000000d * (MILLIS_IN_DAY / GAME_DAY_IRL))));
        }
    }

    @Override
    public void copySnapshotRun(int sequence) {
        // TODO: modified status
        snapshot.update(this);
    }

    @Override
    public boolean checkSequence(TickStage stage, int sequence) {
        if (stage == TickStage.SNAPSHOT) {
            return sequence == 0;
        }
        return sequence == -1;
    }

    @Override
    public Thread getExecutionThread() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExecutionThread(Thread t) {
    }

    private static ShortBitMask STAGES = TickStage.allOf(TickStage.STAGE1, TickStage.SNAPSHOT);
    @Override
    public ShortBitMask getTickStages() {
        return STAGES;
    }

    public void setChunk(int x, int y, int z, int[] blocks) {
        FlowRegion region = getRegionFromChunk(x, y, z, LoadOption.LOAD_GEN);
        region.setChunk(x, y, z, blocks);
    }
}
