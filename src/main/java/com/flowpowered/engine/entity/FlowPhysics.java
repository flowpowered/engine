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
import com.flowpowered.api.entity.Physics;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.engine.geo.region.FlowRegion;
import com.flowpowered.engine.util.math.ReactConverter;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;

import org.spout.physics.body.MobileRigidBody;
import org.spout.physics.body.RigidBody;
import org.spout.physics.body.RigidBodyMaterial;
import org.spout.physics.collision.shape.CollisionShape;

/**
 * The Flow implementation of {@link Physics}. <p/> //TODO: Physics rotation setters
 */
public class FlowPhysics extends Physics {
    //Flow
    private AtomicReference<Transform> snapshot = new AtomicReference<>(Transform.EMPTY);
    private AtomicReference<Transform> live = new AtomicReference<>(Transform.EMPTY);
    //React
    private RigidBody body;
    private final RigidBodyMaterial material = new RigidBodyMaterial();
    //Used in handling crossovers
    private CollisionShape shape;
    private float mass = 0;
    private boolean activated = false;
    private boolean isMobile = true;
    private boolean isGhost = false;

    public FlowPhysics(Entity entity) {
        super(entity);
    }

    @Override
    public FlowPhysics activate(final float mass, final CollisionShape shape, final boolean isGhost, final boolean isMobile) {
        if (mass < 1f) {
            throw new IllegalArgumentException("Cannot activate physics with mass less than 1f");
        }
        if (shape == null) {
            throw new IllegalArgumentException("Cannot activate physics with a null shape");
        }
        if (body != null) {
            ((FlowRegion) entity.getRegion()).removeBody(body);
        }
        this.isGhost = isGhost;
        this.isMobile = isMobile;
        this.mass = mass;
        this.shape = shape;
        activated = true;
        activate((FlowRegion) entity.getRegion());

        return this;
    }

    public void activate(final FlowRegion region) {
        body = region.addBody(live.get(), mass, shape, isGhost, isMobile);
        body.setMaterial(material);
        body.setUserPointer(entity);
    }

    @Override
    public void deactivate() {
        if (entity != null && entity.getRegion() != null && body != null) {
            ((FlowRegion) entity.getRegion()).removeBody(body);
        }
        activated = false;
    }

    @Override
    public boolean isActivated() {
        return activated;
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
    public World getWorld() {
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
    public FlowPhysics impulse(Vector3f impulse, Vector3f offset) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FlowPhysics impulse(Vector3f impulse) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FlowPhysics force(Vector3f force, boolean ignoreGravity) {
        if (body == null) {
            throw new IllegalStateException("Cannot force a null body. If the entity is activated, make sure it is spawned as well");
        }
        if (ignoreGravity) {
            body.setExternalForce(ReactConverter.toReactVector3(force));
        } else {
            body.getExternalForce().add(ReactConverter.toReactVector3(force));
        }
        return this;
    }

    @Override
    public FlowPhysics force(Vector3f force) {
        return force(force, false);
    }

    @Override
    public FlowPhysics torque(Vector3f torque) {
        if (body == null) {
            throw new IllegalStateException("Cannot torque a null body. If the entity is activated, make sure it is spawned as well");
        }
        body.setExternalTorque(ReactConverter.toReactVector3(torque));
        return this;
    }

    @Override
    public FlowPhysics impulseTorque(Vector3f torque) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FlowPhysics dampenMovement(float damp) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public FlowPhysics dampenRotation(float damp) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public float getMass() {
        return mass;
    }

    @Override
    public FlowPhysics setMass(float mass) {
        if (!isActivated()) {
            throw new IllegalStateException("Entities cannot have mass until they are activated");
        }
        if (!(body instanceof MobileRigidBody)) {
            throw new IllegalStateException("Only mobile entities can change mass");
        }
        if (mass < 0f) {
            throw new IllegalArgumentException("Cannot set a mass less than 0f");
        }
        this.mass = mass;
        ((MobileRigidBody) body).setMass(mass);
        return this;
    }

    @Override
    public float getFriction() {
        return material.getFriction();
    }

    @Override
    public FlowPhysics setFriction(float friction) {
        if (friction < 0f || friction > 1f) {
            throw new IllegalArgumentException("Friction must be between 0f and 1f (inclusive)");
        }
        material.setFriction(friction);
        return this;
    }

    @Override
    public float getRestitution() {
        return material.getRestitution();
    }

    @Override
    public FlowPhysics setRestitution(float restitution) {
        if (restitution < 0f || restitution > 1f) {
            throw new IllegalArgumentException("Restitution must be between 0f and 1f (inclusive)");
        }
        material.setRestitution(restitution);
        return this;
    }

    @Override
    public Vector3f getMovementVelocity() {
        if (body == null) {
            throw new IllegalStateException("Cannot get velocity of a null body. If the entity is activated, make sure it is spawned as well");
        }
        return ReactConverter.toFlowVector3(body.getLinearVelocity());
    }

    @Override
    public FlowPhysics setMovementVelocity(Vector3f velocity) {
        if (body == null) {
            throw new IllegalStateException("Cannot set velocity of a null body. If the entity is activated, make sure it is spawned as well");
        }
        if (!(body instanceof MobileRigidBody)) {
            throw new UnsupportedOperationException("Bodies which are not instances of MobileRigidBody cannot set their movement velocity");
        }
        ((MobileRigidBody) body).setLinearVelocity(ReactConverter.toReactVector3(velocity));
        return this;
    }

    @Override
    public Vector3f getRotationVelocity() {
        if (body == null) {
            throw new IllegalStateException("Cannot get rotation velocity of a null body. If the entity is activated, make sure it is spawned as well");
        }
        return ReactConverter.toFlowVector3(body.getAngularVelocity());
    }

    @Override
    public FlowPhysics setRotationVelocity(Vector3f velocity) {
        if (body == null) {
            throw new IllegalStateException("Cannot set rotation velocity of a null body. If the entity is activated, make sure it is spawned as well");
        }
        if (!(body instanceof MobileRigidBody)) {
            throw new UnsupportedOperationException("Bodies which are not instances of MobileRigidBody cannot set their rotation velocity");
        }
        ((MobileRigidBody) body).setAngularVelocity(ReactConverter.toReactVector3(velocity));
        return this;
    }

    @Override
    public CollisionShape getShape() {
        return shape;
    }

    @Override
    public boolean isMobile() {
        return isMobile;
    }

    @Override
    public boolean isGhost() {
        return isGhost;
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
        final org.spout.physics.math.Vector3 positionLiveToReact = ReactConverter.toReactVector3(live.get().getPosition().getVector());
        body.getTransform().setPosition(positionLiveToReact);
    }

    /**
     * Called after the simulation was polled for an update. <p> This updates Flow's live with the transform of the body. The render transform is updated with interpolation from the body </p>
     */
    public void onPostPhysicsTick(float dt) {
        Transform oldLive;
        Transform newLive;
        do {
            oldLive = live.get();
            final Transform physicsLive = ReactConverter.toFlowTransform(body.getTransform(), oldLive.getPosition().getWorld(), oldLive.getScale());
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
