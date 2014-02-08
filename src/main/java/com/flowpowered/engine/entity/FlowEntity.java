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
package com.flowpowered.engine.entity;

import java.util.UUID;

import com.flowpowered.api.Engine;
import com.flowpowered.api.component.BaseComponentOwner;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.EntitySnapshot;
import com.flowpowered.api.entity.Physics;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.geo.region.FlowRegion;

public class FlowEntity extends BaseComponentOwner implements Entity {
    private final int id;
    private FlowPhysics physics;

    private Network network;

    public FlowEntity(Engine engine, int id, Transform transform) {
        super(engine);
        this.id = id;
        this.physics = new FlowPhysics(this);
        this.physics.setTransform(transform);
        this.physics.copySnapshot();
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
    public FlowChunk getChunk() {
        Chunk chunk = physics.getPosition().getChunk(LoadOption.NO_LOAD);
        return (FlowChunk) chunk;
    }

    @Override
    public FlowRegion getRegion() {
        Region region = physics.getPosition().getRegion(LoadOption.LOAD_GEN);
        return (FlowRegion) region;
    }

    @Override
    public EntitySnapshot snapshot() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void tick(float dt) {
        for (Component c : values()) {
            c.tick(dt/1000);
        }
    }

    @Override
    public World getWorld() {
        return physics.getPosition().getWorld();
    }

    void finalizeRun() {
        FlowRegion regionLive = getRegion();
        FlowRegion regionSnapshot = (FlowRegion) physics.getSnapshottedTransform().getPosition().getRegion(LoadOption.LOAD_GEN);
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
