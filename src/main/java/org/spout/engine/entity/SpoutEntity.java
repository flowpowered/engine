package org.spout.engine.entity;

import java.util.Collection;
import java.util.UUID;

import com.flowpowered.commons.datatable.ManagedHashMap;
import com.flowpowered.commons.datatable.ManagedMap;

import org.spout.api.Engine;
import org.spout.api.component.Component;
import org.spout.api.entity.Entity;
import org.spout.api.entity.EntitySnapshot;
import org.spout.api.entity.Physics;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.geo.discrete.Transform;
import org.spout.engine.geo.chunk.SpoutChunk;
import org.spout.engine.geo.region.SpoutRegion;

public class SpoutEntity implements Entity {
    private final int id;
    private SpoutPhysics physics;
    private ManagedHashMap data;

    private Network network;

    public SpoutEntity(int id, Transform transform) {
        this.id = id;
        this.physics = new SpoutPhysics(this);
        this.physics.setTransform(transform);
        this.physics.copySnapshot();
        this.data = new ManagedHashMap();
        this.network = new Network(this);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public UUID getUID() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Engine getEngine() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public void setSavable(boolean savable) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSavable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SpoutChunk getChunk() {
        Chunk chunk = physics.getPosition().getChunk(LoadOption.NO_LOAD);
        return (SpoutChunk) chunk;
    }

    @Override
    public SpoutRegion getRegion() {
        Region region = physics.getPosition().getRegion(LoadOption.NO_LOAD);
        return (SpoutRegion) region;
    }

    @Override
    public EntitySnapshot snapshot() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ManagedMap getData() {
        return data;
    }

    @Override
    public void onTick(float dt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean canTick() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void tick(float dt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public World getWorld() {
        return physics.getPosition().getWorld();
    }

    @Override
    public <T extends Component> T add(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends Component> T get(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends Component> Collection<T> getAll(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> Collection<T> getAllOfType(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends Component> T getExact(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> T getType(Class<T> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T extends Component> T detach(Class<? extends Component> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Component> values() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void finalizeRun() {
		SpoutChunk chunkLive = getChunk();
		SpoutChunk chunkSnapshot = (SpoutChunk) physics.getSnapshottedTransform().getPosition().getChunk(LoadOption.NO_LOAD);
        SpoutRegion regionLive = getRegion();
        SpoutRegion regionSnapshot = (SpoutRegion) physics.getSnapshottedTransform().getPosition().getRegion(LoadOption.NO_LOAD);
        EntityManager entityManager = regionSnapshot != null ? regionSnapshot.getEntityManager() : null;
		//Move entity from Region A to Region B
        boolean activated = false;
        if (chunkSnapshot != null && (chunkLive == null || chunkSnapshot.getRegion() != chunkLive.getRegion())) {
            activated = physics.isActivated();
            physics.deactivate();
            entityManager.removeEntity(this);  
        }
        //Get the new EntityManager for the new region
        entityManager = regionLive != null ? regionLive.getEntityManager() : null;
        if (chunkLive != null && (chunkSnapshot == null || chunkSnapshot.getRegion() != chunkLive.getRegion())) {
            //Add entity to Region B
            entityManager.addEntity(this);
            if (activated) {
                physics.activate(chunkLive.getRegion());
            }
        }

        network.finalizeRun(physics.getTransform());
    }

    void preSnapshotRun() {
        network.preSnapshotRun(physics.getTransform());
    }

    void copySnapshot() {
        physics.copySnapshot();
        network.copySnapshot();
    }

    @Override
    public Physics getPhysics() {
        return physics;
    }

}
