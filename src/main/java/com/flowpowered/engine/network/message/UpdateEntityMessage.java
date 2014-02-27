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
package com.flowpowered.engine.network.message;

import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.player.reposition.RepositionManager;
import com.flowpowered.networking.Message;

public class UpdateEntityMessage implements Message {
	private final int entityId;
	private final Transform transform;
	private final UpdateAction action;

	// TODO: protocol - implement position-only update
	public UpdateEntityMessage(int entityId, Transform transform, UpdateAction action, RepositionManager rm) {
		this.entityId = entityId;
		this.transform = transform;
		this.action = action;
	}

	public UpdateEntityMessage(Entity entity, Transform transform, UpdateAction action, RepositionManager rm) {
		this.entityId = entity.getId();
		this.transform = transform;
		this.action = action;
	}

	public int getEntityId() {
		return entityId;
	}

	/**
	 * @return a copy of the converted transform
	 */
	public Transform getTransform() {
		return transform;
	}

	public UpdateAction getAction() {
		return action;
	}

	public enum UpdateAction {
		// TODO; protocol - use UpdatAction.POSITION?
		/**
		 * Signals for the client to spawn a new entity. (S -> C)
		 */
		ADD,
		/**
		 * Signals for the engine to update the entity's transform. S -> C for all entities. C -> S for players (to verify client movement)
		 */
		TRANSFORM,
		/**
		 * Signals for the engine to update the entity's position. S -> C for all entities. C -> S for players (to verify client movement)
		 *
		 * CURRENTLY UNIMPLEMENTED - deprecated until implemented
		 */
		@Deprecated
		POSITION,
		/**
		 * Signals the client to remove the entity. (S -> C)
		 */
		REMOVE;
		
		public boolean isUpdate() {
			return this == POSITION || this == TRANSFORM;
		}
	}
}
