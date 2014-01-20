/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package com.flowpowered.engine.entity;

import com.flowpowered.api.entity.Physics;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.engine.geo.region.SpoutRegion;
import com.flowpowered.engine.util.math.ReactConverter;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import org.spout.physics.body.MobileRigidBody;
import org.spout.physics.body.RigidBody;
import org.spout.physics.body.RigidBodyMaterial;
import org.spout.physics.collision.shape.CollisionShape;

/**
 * The Spout implementation of {@link com.flowpowered.api.component.entity.SpoutPhysics}. <p/> //TODO: Physics rotation setters
 */
public class SpoutPhysics extends Physics {
	//Spout
	private final Transform snapshot = new Transform();
	private final Transform live = new Transform();
	//React
	private RigidBody body;
	private final RigidBodyMaterial material = new RigidBodyMaterial();
	//Used in handling crossovers
	private CollisionShape shape;
	private float mass = 0;
	private boolean activated = false;
	private boolean isMobile = true;
	private boolean isGhost = false;

    public SpoutPhysics(Entity entity) {
        super(entity);
    }

	@Override
	public SpoutPhysics activate(final float mass, final CollisionShape shape, final boolean isGhost, final boolean isMobile) {
		if (mass < 1f) {
			throw new IllegalArgumentException("Cannot activate physics with mass less than 1f");
		}
		if (shape == null) {
			throw new IllegalArgumentException("Cannot activate physics with a null shape");
		}
		if (body != null) {
			((SpoutRegion) entity.getRegion()).removeBody(body);
		}
		this.isGhost = isGhost;
		this.isMobile = isMobile;
		this.mass = mass;
		this.shape = shape;
		activated = true;
		activate((SpoutRegion) entity.getRegion());

		return this;
	}

	public void activate(final SpoutRegion region) {
		body = region.addBody(live, mass, shape, isGhost, isMobile);
		body.setMaterial(material);
		body.setUserPointer(entity);
	}

	@Override
	public void deactivate() {
		if (entity != null && entity.getRegion() != null && body != null) {
			((SpoutRegion) entity.getRegion()).removeBody(body);
		}
		activated = false;
	}

	@Override
	public boolean isActivated() {
		return activated;
	}

	@Override
	public Transform getSnapshottedTransform() {
		return snapshot.copy();
	}

    @Override
	public Transform getTransform() {
		return live.copy();
	}

	@Override
	public SpoutPhysics setTransform(Transform transform) {
		return setTransform(transform, true);
	}

	@Override
	public SpoutPhysics setTransform(Transform transform, boolean sync) {
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
		return live.getPosition();
	}

	@Override
	public SpoutPhysics setPosition(Point point) {
		live.setPosition(point);
		return this;
	}

	@Override
	public boolean isPositionDirty() {
		return !snapshot.getPosition().equals(live.getPosition());
	}

	@Override
	public Quaternionf getRotation() {
		return live.getRotation();
	}

	@Override
	public SpoutPhysics setRotation(Quaternionf rotation) {
		if (rotation == null) {
			throw new IllegalArgumentException("rotation cannot be null!");
		}
		live.setRotation(rotation);
		sync();
		return this;
	}

	@Override
	public boolean isRotationDirty() {
		return !snapshot.getRotation().equals(live.getRotation());
	}

	@Override
	public Vector3f getScale() {
		return live.getScale();
	}

	@Override
	public SpoutPhysics setScale(Vector3f scale) {
		if (scale == null) {
			throw new IllegalArgumentException("scale cannot be null!");
		}
		live.setScale(scale);
		sync();
		return this;
	}

	@Override
	public boolean isScaleDirty() {
		return !snapshot.getScale().equals(live.getScale());
	}

	@Override
	public World getWorld() {
		return getPosition().getWorld();
	}

	@Override
	public boolean isWorldDirty() {
		return !snapshot.getPosition().getWorld().equals(live.getPosition().getWorld());
	}

	@Override
	public SpoutPhysics translate(Vector3f point) {
		live.translate(point);
		return this;
	}

	@Override
	public SpoutPhysics rotate(Quaternionf rotate) {
		if (rotate == null) {
			throw new IllegalArgumentException("rotate cannot be null!");
		}
		live.rotate(rotate);
		sync();
		return this;
	}

	@Override
	public SpoutPhysics scale(Vector3f scale) {
		if (scale == null) {
			throw new IllegalArgumentException("scale cannot be null!");
		}
		live.scale(scale);
		sync();
		return this;
	}

	@Override
	public SpoutPhysics impulse(Vector3f impulse, Vector3f offset) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public SpoutPhysics impulse(Vector3f impulse) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public SpoutPhysics force(Vector3f force, boolean ignoreGravity) {
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
	public SpoutPhysics force(Vector3f force) {
		return force(force, false);
	}

	@Override
	public SpoutPhysics torque(Vector3f torque) {
		if (body == null) {
			throw new IllegalStateException("Cannot torque a null body. If the entity is activated, make sure it is spawned as well");
		}
		body.setExternalTorque(ReactConverter.toReactVector3(torque));
		return this;
	}

	@Override
	public SpoutPhysics impulseTorque(Vector3f torque) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public SpoutPhysics dampenMovement(float damp) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public SpoutPhysics dampenRotation(float damp) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public float getMass() {
		return mass;
	}

	@Override
	public SpoutPhysics setMass(float mass) {
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
	public SpoutPhysics setFriction(float friction) {
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
	public SpoutPhysics setRestitution(float restitution) {
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
		return ReactConverter.toSpoutVector3(body.getLinearVelocity());
	}

	@Override
	public SpoutPhysics setMovementVelocity(Vector3f velocity) {
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
		return ReactConverter.toSpoutVector3(body.getAngularVelocity());
	}

	@Override
	public SpoutPhysics setRotationVelocity(Vector3f velocity) {
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
	 * Called before the simulation is polled for an update. <p> This aligns the body's transform with Spout's if someone moves without physics. </p>
	 */
	public void onPrePhysicsTick() {
		if (body == null) {
			return;
		}
		final org.spout.physics.math.Vector3 positionLiveToReact = ReactConverter.toReactVector3(live.getPosition());
		body.getTransform().setPosition(positionLiveToReact);
	}

	/**
	 * Called after the simulation was polled for an update. <p> This updates Spout's live with the transform of the body. The render transform is updated with interpolation from the body </p>
	 */
	public void onPostPhysicsTick(float dt) {
			final Transform physicsLive = ReactConverter.toSpoutTransform(body.getTransform(), live.getPosition().getWorld(), live.getScale());
			if (!live.equals(physicsLive)) {
				live.set(physicsLive);
				sync();
			}
	}

	public void copySnapshot() {
		snapshot.set(live);
	}

	private void sync() {
	}
}
