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
package com.flowpowered.api.entity;

import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.discrete.TransformProvider;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.api.util.thread.annotation.SnapshotRead;
import com.flowpowered.api.util.thread.annotation.Threadsafe;
import org.spout.physics.collision.shape.CollisionShape;

/**
 * Component that gives the owner the characteristics to be a part of a Scene. <p> A Scene consists of {@link Transform}s which represent the snapshot state, the live state, and the rendering state.
 * This component can be used to manipulate the object within the scene.
 */
public abstract class Physics implements TransformProvider {
    protected final Entity entity;

    public Physics(Entity entity) {
        this.entity = entity;
    }
    /**
     * Activates this {@link com.flowpowered.api.entity.Entity}, inserting it into the physics space <p/> If the entity was already in the physics space, calling this will incur a removal and reinsertion.
     *
     * @param mass The mass of the entity
     * @param shape The collidable shape of the entity
     * @param isGhost Is this entity a detector "ghost" object
     * @param isMobile Is this entity mobile (will it ever move)
     * @return This component, for chaining
     * @throws IllegalArgumentException If mass is < 0f or shape is null
     */
    public abstract Physics activate(final float mass, final CollisionShape shape, final boolean isGhost, final boolean isMobile);

    /**
     * Deactivates this {@link com.flowpowered.api.entity.Entity}, removing it from the physics space
     */
    public abstract void deactivate();

    /**
     * Returns whether physics is active or not. <p/> Physics is considered activated when given a nonzero mass.
     *
     * @return activated
     */
    public abstract boolean isActivated();

    // Transform

    /**
     * Gets the {@link Transform} this {@link com.flowpowered.api.entity.Entity} had within the last game tick. <p/> The Transform is stable, it is completely impossible for it to be updated.
     *
     * @return The Transform as of the last game tick.
     */
    @SnapshotRead
    public abstract Transform getSnapshottedTransform();

    /**
     * Sets the {@link Transform} for this {@link com.flowpowered.api.entity.Entity}. <p> This function sets the live state of the entity's transform, not the snapshot state. As such, its advised to set the
     * transform lastly else retrieving the transform afterwards within the same tick will not return expected values (due to potential other plugin changes as well as {@link #getSnapshottedTransform()) returning
     * snapshot state). This is equivalent to calling {@link #setTransform(transform, true)}.
     *
     * @param transform The new live transform state of this entity.
     * @return This component, for chaining.
     */
    public abstract Physics setTransform(Transform transform);

    /**
     * Sets the {@link Transform} for this {@link com.flowpowered.api.entity.Entity}. <p> This function sets the live state of the entity's transform, not the snapshot state. As such, its advised to set the
     * transform lastly else retrieving the transform afterwards within the same tick will not return expected values (due to potential other plugin changes as well as {@link #getSnapshottedTransform()) returning
     * snapshot state).
     *
     * @param transform The new live transform state of this entity.
     * @param sync Whether or not to sync the changes with the client
     * @return This component, for chaining.
     */
    public abstract Physics setTransform(Transform transform, boolean sync);

    /**
     * Returns whether the live transform and snapshot transform are not equal.
     *
     * @return True if live is different than snapshot, false if the same.
     */
    public abstract boolean isTransformDirty();

    /**
     * Gets the {@link Point} representing the location where the {@link com.flowpowered.api.entity.Entity} is. <p/> The Point is guaranteed to always be valid.
     *
     * @return The Point
     */
    @SnapshotRead
    @Threadsafe
    public abstract Point getPosition();

    /**
     * Sets the {@link Point} for this {@link com.flowpowered.api.entity.Entity}. This will directly set both the world space of the Entity as well as physics space. The physics space will be cleared of all
     * forces in the process. <p> This function sets the live state of the entity's point, not the snapshot state. As such, its advised to set the point lastly else retrieving the point afterwards within
     * the same tick will not return expected values (due to potential other plugin changes as well as {@link #getPosition()) returning snapshot state). This method always syncs the change to the client.
     *
     * @param point The new live position state of this entity.
     * @return This component, for chaining.
     */
    public abstract Physics setPosition(Point point);

    /**
     * Determines if this {@link com.flowpowered.api.entity.Entity} has a dirty position. <p> A dirty position is when the snapshot position (last tick) and live position are not equal.
     *
     * @return True if position is dirty, false if not.
     */
    public abstract boolean isPositionDirty();

    /**
     * Gets the {@link com.flowpowered.math.imaginary.Quaternionf} representing the rotation of the {@link com.flowpowered.api.entity.Entity}. <p/> The Quaternion is guaranteed to always be valid.
     *
     * @return The Quaternion
     */
    @SnapshotRead
    @Threadsafe
    public abstract Quaternionf getRotation();

    /**
     * Sets the {@link com.flowpowered.math.imaginary.Quaternionf} for this {@link com.flowpowered.api.entity.Entity}. <p> This functions sets the live state of the entity's quaternion (rotation), not the snapshot state. As such, its advised
     * to set the quaternion lastly else retrieving the quaternion afterwards within the same tick will not return expected values (due to potential other plugin changes as well as {@link #getRotation())
     * returning snapshot state). This method always syncs the change to the client.
     *
     * @param rotation The new live quaternion (rotation) of this entity.
     * @return This component, for chaining.
     */
    public abstract Physics setRotation(Quaternionf rotation);

    /**
     * Determines if this {@link com.flowpowered.api.entity.Entity} has a dirty rotation. <p> A dirty rotation is when the snapshot rotation (last tick) and live rotation are not equal.
     *
     * @return True if dirty, false if not.
     */
    public abstract boolean isRotationDirty();

    /**
     * Gets the {@link com.flowpowered.math.vector.Vector3f} representing the scale of the {@link com.flowpowered.api.entity.Entity}. <p/> The Scale is guaranteed to always be valid.
     *
     * @return The Scale (Vector3).
     */
    @SnapshotRead
    @Threadsafe
    public abstract Vector3f getScale();

    /**
     * Sets the {@link com.flowpowered.math.vector.Vector3f} representing the scale of the {@link com.flowpowered.api.entity.Entity}. <p> This functions sets the live state of the entity's scale, not the snapshot state. As such, its
     * advised to set the scale lastly else retrieving the scale afterwards within the same tick will not return expected values (due to potential other plugin changes as well as {@link #getScale())
     * returning snapshot state). This method always syncs the change to the client.
     *
     * @param scale The new live scale of this entity.
     * @return This component, for chaining.
     */
    public abstract Physics setScale(Vector3f scale);

    /**
     * Determines if this {@link com.flowpowered.api.entity.Entity} has a dirty scale. <p> A dirty scale is when the snapshot scale (last tick) and live scale are not equal.
     *
     * @return True if dirty, false if not.
     */
    public abstract boolean isScaleDirty();

    /**
     * Gets the {@link World} in-which this {@link com.flowpowered.api.entity.Entity} is a part of.
     *
     * @return The world this entity is in.
     */
    public World getWorld() {
        return entity.getWorld();
    }

    /**
     * Determines if this {@link com.flowpowered.api.entity.Entity} has a dirty {@link World}. <p> A dirty world is when the snapshot world (last tick) and live world are not equal.
     *
     * @return True if dirty, false if not.
     */
    public abstract boolean isWorldDirty();

    /**
     * Translates this {@link com.flowpowered.api.entity.Entity} from its current {@link Point} to the Point that is the addition of the {@link com.flowpowered.math.vector.Vector3f} provided. <p> For example, if I want to move an Entity
     * up one (Up being the y-axis), I would do a {@code translate(new Vector3(0, 1, 0));}. This method always syncs the change to the client.
     *
     * @param translation A Vector3 which will be added to the current Point (position).
     * @return This component, so you can chain.
     */
    public abstract Physics translate(Vector3f translation);

    /**
     * Rotates this {@link com.flowpowered.api.entity.Entity} from its current {@link com.flowpowered.math.imaginary.Quaternionf} to the Quaternion that is the addition of the Quaternion provided. <p/> For example, if I
     * want to rotate an Entity upwards (which is moving its yaw), I would do a rotate(new Quaternion(0, 1, 0, 0)); <p> Bear in mind, doing a rotate does so without physics and instead the rotation of
     * the Entity will be directly set within its physics transform. This method always syncs the change to the client.
     *
     * @param rotate A Quaternion which will be added to the current Quaternion (rotation).
     * @return This component, so you can chain.
     */
    public abstract Physics rotate(Quaternionf rotate);

    /**
     * Scales this {@link com.flowpowered.api.entity.Entity} from its current scale to the {@link com.flowpowered.math.vector.Vector3f} representing the new scale which is an addition of the Vector3 provided. <p/> For example, if I want
     * to scale an Entity to be taller (which is scaling its y-factor), I would do a {@code scale(new Vector3(0, 1, 0));}. This method always syncs the change to the client.
     *
     * @param scale A Vector3 which will be added to the current Vector3 (scale).
     * @return This component, so you can chain.
     */
    public abstract Physics scale(Vector3f scale);

    // Physics

    /**
     * Impulse is a force across delta time (since last simulation tick). A few rules apply. <p/> - The entity must have a mass > 0 (ie not a static object). - The offset is in world space. This means
     * the impulse is applied from the offset provided. - Entities of higher masses need greater impulses to move. Can't get movement to occur? Lower mass or apply greater impulse. <p> For example, if I
     * want to propel an entity forward, like a wave of water pushing an entity downstream, I would do the following. <p> // Vector3.FORWARD = 0, 0, 1 physics.impulse(Vector3.FORWARD, new
     * Vector3(getPosition().subtract(getSnapshottedTransform().getForward())); // The above adds 1 to the forwardness of the Entity every simulation step (if applied in onTick for example)
     *
     * @param impulse The Vector3 impulse (force) to apply.
     * @param offset The offset within the world to apply the impulse from.
     * @return This component, so you can chain.
     */
    public abstract Physics impulse(Vector3f impulse, Vector3f offset);

    /**
     * Impulse is a force across delta time (since last simulation tick). A few rules apply. <p/> - The entity must have a mass > 0 (ie not a static object). - The offset is in world space. This means
     * the impulse is applied from the offset provided. - Entities of higher masses need greater impulses to move. Can't get movement to occur? Lower mass or apply greater impulse. <p/> For example, if I
     * want to propel an entity forward, like a wave of water pushing an entity downstream, I would do the following. <p/> // Vector3.FORWARD = 0, 0, 1 physics.impulse(Vector3.FORWARD, new
     * Vector3(getPosition().subtract(getSnapshottedTransform().getForward())); // The above adds 1 to the forwardness of the Entity every simulation step (if applied in onTick for example)
     *
     * @param impulse The Vector3 impulse (force) to apply.
     * @return This component, so you can chain.
     */
    public abstract Physics impulse(Vector3f impulse);

    /**
     * Force is, as it sounds, an instant force to the Entity. A few rules apply. <p/> - The entity must be mobile (mass > 0) - Entities of higher masses need greater forces to move <p/> Can't get
     * movement to occur? Lower the mass or increase the force.
     *
     * @param force The Vector3 force to apply.
     * @param ignoreGravity True to force without gravity, false to have gravity affect the force
     * @return This component, so you can chain.
     */
    public abstract Physics force(Vector3f force, boolean ignoreGravity);

    /**
     * Force is, as it sounds, an instant force to the Entity. A few rules apply. <p/> - The entity must be mobile (mass > 0) - Entities of higher masses need greater forces to move <p/> Can't get
     * movement to occur? Lower the mass or increase the force. Lastly, this method forces with gravity (it doesn't ignore it).
     *
     * @param force The Vector3 force to apply.
     * @return This component, so you can chain.
     */
    public abstract Physics force(Vector3f force);

    /**
     * Torque performs a rotation of the {@link com.flowpowered.api.entity.Entity} by the {@link com.flowpowered.math.vector.Vector3f}. <p> The Vector3 is (yaw, pitch, roll) and is an instant change to rotation. <p> For example, if I want
     * to rotate the entity to the right, I would do the following. <p> //Vector3.RIGHT = 1, 0, 0 scene.torque(Vector3.RIGHT); //The above rotates the Entity to the right instantly.
     *
     * @param torque The Vector3 torque to apply
     * @return This component, so you can chain.
     */
    public abstract Physics torque(Vector3f torque);

    /**
     * Impulse Torque performs a rotation over a delta of the {@link com.flowpowered.api.entity.Entity} by the {@link com.flowpowered.math.vector.Vector3f}. <p> The Vector3 is (yaw, pitch, roll) and is a change in rotation spread over
     * delta time (since last simulation). <p> For example, if I want to rotate the entity to the right over time, I would do the following. <p> //Vector3.RIGHT = 1, 0, 0
     * scene.impulseTorque(Vector3.RIGHT) //The above rotates the Entity over time to the right.
     *
     * @param torque Tne Vector3 torque to apply.
     * @return This component, so you can chain.
     */
    public abstract Physics impulseTorque(Vector3f torque);

    /**
     * Dampens the {@link com.flowpowered.api.entity.Entity}'s movement velocity by the factor provided. <p> 0.0f = no dampening, 1.0f = full velocity stop. Any values outside this range will be clamped to the
     * nearest bound.
     *
     * @param damp The float dampener to apply.
     * @return This component, so you can chain.
     */
    public abstract Physics dampenMovement(float damp);

    /**
     * Dampens the {@link com.flowpowered.api.entity.Entity}'s rotation velocity by the factor provided. <p> 0.0f = no dampening, 1.0f = full velocity stop. Any values outside this range will be clamped to the
     * nearest bound.
     *
     * @param damp The float dampener to apply.
     * @return This component, so you can chain.
     */
    public abstract Physics dampenRotation(float damp);

    // Physics characteristics

    /**
     * Gets the mass (heaviness) of this {@link com.flowpowered.api.entity.Entity}.
     *
     * @return The mass
     */
    public abstract float getMass();

    /**
     * Sets the mass (heaviness) of this {@link com.flowpowered.api.entity.Entity}.
     *
     * @param mass The new mass
     * @return This component, for chaining
     * @throws IllegalArgumentException If mass provided is < 0f
     */
    public abstract Physics setMass(final float mass);

    /**
     * Gets the friction (slipperiness) of this {@link com.flowpowered.api.entity.Entity}. <p> Values range between 0f and 1f where 0f means no friction
     *
     * @return The friction
     */
    public abstract float getFriction();

    /**
     * Sets the friction (slipperiness) of this {@link com.flowpowered.api.entity.Entity}.
     *
     * @param friction The new friction
     * @return This component, for chaining
     * @throws IllegalArgumentException If friction provided is less than 0f or greater than 1f
     */
    public abstract Physics setFriction(final float friction);

    /**
     * Gets the restitution (bounciness) of this {@link com.flowpowered.api.entity.Entity}. <p> Values range between 0f and 1f where 0f means no bouncing
     *
     * @return The restitution
     */
    public abstract float getRestitution();

    /**
     * Sets the restitution (bounciness) of this {@link com.flowpowered.api.entity.Entity}.
     *
     * @param restitution The new restitution
     * @return This component, for chaining
     * @throws IllegalArgumentException If restitution provided is less than 0f or greater than 1f
     */
    public abstract Physics setRestitution(final float restitution);

    /**
     * Gets the movement velocity of this {@link com.flowpowered.api.entity.Entity}.
     *
     * @return the current velocity as a {@link com.flowpowered.math.vector.Vector3f}.
     */
    public abstract Vector3f getMovementVelocity();

    /**
     * Sets the movement velocity for this {@link com.flowpowered.api.entity.Entity}.
     *
     * @param velocity The {@link com.flowpowered.math.vector.Vector3f} velocity to apply to movement.
     * @return This component, so you can chain.
     */
    public abstract Physics setMovementVelocity(Vector3f velocity);

    /**
     * Gets rotation velocity of this {@link com.flowpowered.api.entity.Entity}.
     *
     * @return the current velocity as a {@link com.flowpowered.math.vector.Vector3f}.
     */
    public abstract Vector3f getRotationVelocity();

    /**
     * Sets the rotation velocity for this {@link com.flowpowered.api.entity.Entity}.
     *
     * @param velocity The {@link com.flowpowered.math.vector.Vector3f} velocity to apply to rotation.
     * @return This component, so you can chain.
     */
    public abstract Physics setRotationVelocity(Vector3f velocity);

    /**
     * Returns the {@link CollisionShape} this {@link com.flowpowered.api.entity.Entity} has
     *
     * @return The collision shape
     */
    public abstract CollisionShape getShape();

    /**
     * Returns whether the {@link com.flowpowered.api.entity.Entity} is activated with mobile status. <p> By default all entities are considered "mobile". This setting also does not prohibit manual movement
     * control.
     *
     * @return True if mobile, false if not
     */
    public abstract boolean isMobile();

    /**
     * Returns whether the {@link com.flowpowered.api.entity.Entity} is activated with ghost status. <p> By default all entities are not ghosts and this simply means that the body will alert all other bodies
     * of collisions but this body will neither inccur a collision nor stop the other bodies from passing through.
     *
     * @return True if ghost, false if not
     */
    public abstract boolean isGhost();

    @Override
    public abstract Transform getTransform();
}
