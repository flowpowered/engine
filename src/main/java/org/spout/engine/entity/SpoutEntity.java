package org.spout.engine.entity;

import java.util.Collection;
import java.util.UUID;

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

public class SpoutEntity implements Entity {
    private final int id;
    private Transform location;
    private SpoutPhysics physics;

    public SpoutEntity(int id, Transform transform) {
        this.id = id;
        this.location = transform;
        this.physics = new SpoutPhysics(this);
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
        throw new UnsupportedOperationException("Not supported yet.");
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
    public Chunk getChunk() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getRegion() {
        return location.getPosition().getRegion(LoadOption.LOAD_GEN);
    }

    @Override
    public EntitySnapshot snapshot() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ManagedMap getData() {
        throw new UnsupportedOperationException("Not supported yet.");
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
        return location.getPosition().getWorld();
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
    }

    void preSnapshotRun() {
    }

    void copySnapshot() {
    }

    @Override
    public Physics getPhysics() {
        return physics;
    }

}
