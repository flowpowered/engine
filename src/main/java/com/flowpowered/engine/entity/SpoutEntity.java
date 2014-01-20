package com.flowpowered.engine.entity;

import java.util.Collection;
import java.util.UUID;

import com.flowpowered.commons.datatable.ManagedHashMap;
import com.flowpowered.commons.datatable.ManagedMap;

import com.flowpowered.api.Engine;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.EntitySnapshot;
import com.flowpowered.api.entity.Physics;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.engine.geo.chunk.SpoutChunk;
import com.flowpowered.engine.geo.region.SpoutRegion;

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
        Region region = physics.getPosition().getRegion(LoadOption.LOAD_GEN);
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
        SpoutRegion regionLive = getRegion();
        SpoutRegion regionSnapshot = (SpoutRegion) physics.getSnapshottedTransform().getPosition().getRegion(LoadOption.LOAD_GEN);
		//Move entity from Region A to Region B
        if (regionSnapshot != regionLive) {
            boolean activated = physics.isActivated();
            physics.deactivate();
            regionSnapshot.getEntityManager().removeEntity(this);

            //Add entity to Region B
            regionLive.getEntityManager().addEntity(this);
            if (activated) {
                physics.activate(regionLive);
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
