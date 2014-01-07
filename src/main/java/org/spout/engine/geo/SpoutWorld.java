package org.spout.engine.geo;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.flowpowered.commons.datatable.ManagedHashMap;
import com.flowpowered.commons.datatable.ManagedMap;
import com.flowpowered.events.Cause;
import org.spout.api.Engine;
import org.spout.api.component.BaseComponentOwner;
import org.spout.api.component.Component;
import org.spout.api.entity.Entity;
import org.spout.api.entity.EntityPrefab;
import org.spout.api.entity.Player;
import org.spout.api.generator.WorldGenerator;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.material.BlockMaterial;
import org.spout.api.scheduler.TaskManager;
import org.spout.api.util.cuboid.CuboidBlockMaterialBuffer;
import org.spout.engine.util.thread.AsyncManager;
import org.spout.engine.util.thread.snapshotable.SnapshotManager;
import org.spout.engine.util.thread.snapshotable.SnapshotableLong;
import org.spout.math.GenericMath;
import org.spout.math.vector.Vector3f;

public class SpoutWorld extends BaseComponentOwner implements World, AsyncManager {
    private final String name;
    private final UUID uid;
    private final SnapshotManager snapshotManager;
    private final SnapshotableLong age;

    public SpoutWorld(String name, UUID uid, long age) {
        this.name = name;
        this.uid = uid;
        this.snapshotManager = new SnapshotManager();
        this.age = new SnapshotableLong(snapshotManager, age);
    }

    public SpoutWorld(String name) {
        this(name, UUID.randomUUID(), 0);
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
    public Entity createEntity(Point point, Class<? extends Component>... classes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity createEntity(Point point, EntityPrefab prefab) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void spawnEntity(Entity e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity createAndSpawnEntity(Point point, LoadOption option, EntityPrefab prefab) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity createAndSpawnEntity(Point point, LoadOption option, Class<? extends Component>... classes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity[] createAndSpawnEntity(Point[] points, LoadOption option, Class<? extends Component>... classes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Engine getEngine() {
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
    public TaskManager getParallelTaskManager() {
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegion(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegion(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegionFromChunk(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegionFromChunk(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegionFromBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegionFromBlock(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegionFromBlock(Vector3f position) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegionFromBlock(Vector3f position, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    @Override
    public Chunk getChunk(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsChunk(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public boolean hasChunk(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasChunkAtBlock(int x, int y, int z) {
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
    public boolean containsBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	@Override
	public SpoutBlock getBlock(int x, int y, int z) {
		return new SpoutBlock(this, x, y, z);
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

    @Override
    public void finalizeRun() {
    }

    @Override
    public void preSnapshotRun() {
    }

    @Override
    public void copySnapshotRun() {
        System.out.println("copySnapshot");
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

    @Override
    public int getMaxStage() {
        return -1;
    }
}
