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

import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.Physics;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.reference.WorldReference;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.util.math.ReactConverter;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import org.spout.physics.body.RigidBody;
import org.spout.physics.collision.shape.CollisionShape;
import org.spout.physics.engine.DynamicsWorld;
import org.spout.physics.engine.Material;
import org.spout.physics.math.Quaternion;

/**
 * The Flow implementation of {@link Physics}. <p/> //TODO: Physics rotation setters
 */
public class FlowPhysics extends Physics {
    //Flow
    private final AtomicReference<Transform> snapshot = new AtomicReference<>(Transform.INVALID);
    private final AtomicReference<Transform> live = new AtomicReference<>(Transform.INVALID);
    //React
    private RigidBody body;
    private boolean activated = false;

    public FlowPhysics(Entity entity) {
        super(entity);
    }

    @Override
    public FlowPhysics activate(final float mass, final CollisionShape shape, DynamicsWorld sim) {
        if (mass < 1f) {
            throw new IllegalArgumentException("Cannot activate physics with mass less than 1f");
        }
        if (shape == null) {
            throw new IllegalArgumentException("Cannot activate physics with a null shape");
        }
        deactivate();
        Transform transform = live.get();
        body = sim.createRigidBody(new org.spout.physics.math.Transform(ReactConverter.toReactVector3(transform.getPosition().getVector()), new Quaternion(0, 0, 0, 1)), mass, shape);
        Material mat = new Material();
        mat.setBounciness(0f);
        mat.setFrictionCoefficient(0f);
        body.setMaterial(mat);
        activated = true;
        return this;
    }

    public void crossInto(final FlowWorld from, final FlowWorld to) {
        if (entity != null && from != null && body != null) {
            from.getPhysicsManager().queuePreUpdateTask((w) -> w.destroyRigidBody(body));
            Material mat = body.getMaterial();
            body = to.getPhysicsManager().getSimulation().createRigidBody(body.getTransform(), body.getMass(), body.getCollisionShape());
            body.setMaterial(mat);
        }
    }

    @Override
    public void deactivate() {
        FlowWorld world =(FlowWorld) entity.getWorld().get();
        if (world != null && body != null) {
            world.getPhysicsManager().queuePreUpdateTask((w) -> w.destroyRigidBody(body));
        }
        body = null;
        activated = false;
    }

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public RigidBody getBody() {
        return body;
    }

    @Override
    public Transform getSnapshottedTransform() {
        return snapshot.get();
    }

    @Override
    public Transform getTransform() {
        return live.get();
    }

    @Override
    public FlowPhysics setTransform(Transform transform) {
        return setTransform(transform, true);
    }

    @Override
    public FlowPhysics setTransform(Transform transform, boolean sync) {
        if (transform == null) {
            throw new IllegalArgumentException("Transform cannot be null!");
        }
        live.set(transform);
        if (sync) {
            sync();
        }
        return this;
    }

    @Override
    public boolean isTransformDirty() {
        return !snapshot.equals(live);
    }

    @Override
    public Point getPosition() {
        return live.get().getPosition();
    }

    @Override
    public FlowPhysics setPosition(Point position) {
        if (position == null) {
            throw new IllegalArgumentException("position cannot be null!");
        }
        Transform oldTransform;
        Transform newTransform;
        do {
            oldTransform = live.get();
            newTransform = oldTransform.withPosition(position);
        } while (!live.compareAndSet(oldTransform, newTransform));
        sync();
        return this;
    }

    @Override
    public boolean isPositionDirty() {
        return !snapshot.get().getPosition().equals(live.get().getPosition());
    }

    @Override
    public Quaternionf getRotation() {
        return live.get().getRotation();
    }

    @Override
    public FlowPhysics setRotation(Quaternionf rotation) {
        if (rotation == null) {
            throw new IllegalArgumentException("rotation cannot be null!");
        }
        Transform oldTransform;
        Transform newTransform;
        do {
            oldTransform = live.get();
            newTransform = oldTransform.withRotation(rotation);
        } while (!live.compareAndSet(oldTransform, newTransform));
        sync();
        return this;
    }

    @Override
    public boolean isRotationDirty() {
        return !snapshot.get().getRotation().equals(live.get().getRotation());
    }

    @Override
    public Vector3f getScale() {
        return live.get().getScale();
    }

    @Override
    public FlowPhysics setScale(Vector3f scale) {
        if (scale == null) {
            throw new IllegalArgumentException("scale cannot be null!");
        }
        Transform oldTransform;
        Transform newTransform;
        do {
            oldTransform = live.get();
            newTransform = oldTransform.withScale(scale);
        } while (!live.compareAndSet(oldTransform, newTransform));
        sync();
        return this;
    }

    @Override
    public boolean isScaleDirty() {
        return !snapshot.get().getScale().equals(live.get().getScale());
    }

    @Override
    public WorldReference getWorld() {
        return getPosition().getWorld();
    }

    @Override
    public boolean isWorldDirty() {
        return !snapshot.get().getPosition().getWorld().equals(live.get().getPosition().getWorld());
    }

    @Override
    public FlowPhysics translate(Vector3f translate) {
        if (translate == null) {
            throw new IllegalArgumentException("translate cannot be null!");
        }
        Transform oldTransform;
        Transform newTransform;
        do {
            oldTransform = live.get();
            newTransform = oldTransform.translated(translate);
        } while (!live.compareAndSet(oldTransform, newTransform));
        sync();
        return this;
    }

    @Override
    public FlowPhysics rotate(Quaternionf rotate) {
        if (rotate == null) {
            throw new IllegalArgumentException("rotate cannot be null!");
        }
        Transform oldTransform;
        Transform newTransform;
        do {
            oldTransform = live.get();
            newTransform = oldTransform.rotated(rotate);
        } while (!live.compareAndSet(oldTransform, newTransform));
        sync();
        return this;
    }

    @Override
    public FlowPhysics scale(Vector3f scale) {
        if (scale == null) {
            throw new IllegalArgumentException("scale cannot be null!");
        }
        Transform oldTransform;
        Transform newTransform;
        do {
            oldTransform = live.get();
            newTransform = oldTransform.scaled(scale);
        } while (!live.compareAndSet(oldTransform, newTransform));
        sync();
        return this;
    }

    @Override
    public String toString() {
        return "snapshot= {" + snapshot + "}, live= {" + live + "}, body= {" + body + "}";
    }

    /**
     * Called before the simulation is polled for an update. <p> This aligns the body's transform with Flow's if someone moves without physics. </p>
     */
    public void onPrePhysicsTick() {
        if (body == null) {
            return;
        }
        org.spout.physics.math.Transform toReact = ReactConverter.toReactTransform(live.get());
        if (!body.getTransform().equals(toReact)) {
            body.setIsSleeping(false);
        }
        body.getTransform().set(toReact);
    }

    /**
     * Called after the simulation was polled for an update. <p> This updates Flow's live with the transform of the body. The render transform is updated with interpolation from the body </p>
     */
    public void onPostPhysicsTick() {
        if (body == null) {
            return;
        }
        Transform oldLive;
        Transform newLive;
        do {
            oldLive = live.get();
            final Transform physicsLive = new Transform(new Point(oldLive.getPosition().getWorld(), ReactConverter.toFlowVector3(body.getTransform().getPosition())), oldLive.getRotation(), oldLive.getScale());
            if (!oldLive.equals(physicsLive)) {
                newLive = physicsLive;
                sync();
            } else {
                newLive = oldLive;
            }
        } while (!live.compareAndSet(oldLive, newLive));
    }

    public void copySnapshot() {
        snapshot.set(live.get());
    }

    private void sync() {
    }
}
