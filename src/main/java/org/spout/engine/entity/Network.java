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
package org.spout.engine.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.commons.datatable.defaulted.DefaultedKey;
import com.flowpowered.commons.datatable.defaulted.DefaultedKeyImpl;

import org.spout.api.Platform;
import org.spout.api.Spout;
import org.spout.api.entity.Entity;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Transform;
import com.flowpowered.math.vector.Vector3i;

/**
 * The networking behind {@link org.spout.api.entity.Entity}s.
 */
public class Network {
	public static final LoadOption LOAD_GEN_NOWAIT = new LoadOption(true, true, false);
	private static final WrappedSerizableIterator INITIAL_TICK = new WrappedSerizableIterator(null);
	//TODO: Move all observer code to Network
	public final DefaultedKey<Boolean> IS_OBSERVER = new DefaultedKeyImpl<>("IS_OBSERVER", false);
	/**
	 * null means use SYNC_DISTANCE and is generated each update; not observing is {@code new OutwardIterator(0, 0, 0)}; custom Iterators can be used for others We want default to be null so that when it
	 * is default observer, it returns null
	 */
	public final DefaultedKey<WrappedSerizableIterator> OBSERVER_ITERATOR = new DefaultedKeyImpl<>("OBSERVER_ITERATOR", null);
	/**
	 * In chunks
	 */
	public final DefaultedKey<Integer> SYNC_DISTANCE = new DefaultedKeyImpl<>("SYNC_DISTANCE", 10);
	private final Set<Chunk> observingChunks = new HashSet<>();
	private AtomicReference<WrappedSerizableIterator> liveObserverIterator = new AtomicReference<>(new WrappedSerizableIterator(new OutwardIterator(0, 0, 0, 0)));
	private boolean observeChunksFailed = false;
    private final Entity entity;

    public Network(Entity entity) {
        this.entity = entity;
        entity.getData().put(IS_OBSERVER, true);
        entity.getData().put(OBSERVER_ITERATOR, INITIAL_TICK);
    }

	public static class WrappedSerizableIterator implements Serializable, Iterator<Vector3i> {
		private static final long serialVersionUID = 1L;
		private final Iterator<Vector3i> object;

		public <T extends Iterator<Vector3i> & Serializable> WrappedSerizableIterator(T object) {
			this.object = object;
		}

		@Override
		public boolean hasNext() {
			return object.hasNext();
		}

		@Override
		public Vector3i next() {
			return object.next();
		}

		@Override
		public void remove() {
			object.remove();
		}

		private void writeObject(ObjectOutputStream stream) throws IOException {
			stream.defaultWriteObject();
		}

		private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
			stream.defaultReadObject();
		}
    }

	/**
	 * Returns if the owning {@link org.spout.api.entity.Entity} is an observer. <p/> Observer means the Entity can trigger network updates (such as chunk creation) within its sync distance.
	 *
	 * @return True if observer, false if not
	 */
	public boolean isObserver() {
		return entity.getData().get(IS_OBSERVER);
	}

	/**
	 * Sets the observer status for the owning {@link org.spout.api.entity.Entity}. If there was a custom observer iterator being used, passing {@code true} will cause it to reset to the default observer
	 * iterator.
	 *
	 * @param observer True if observer, false if not
	 */
	public void setObserver(final boolean observer) {
		entity.getData().put(IS_OBSERVER, observer);
		if (observer) {
			liveObserverIterator.set(null);
		} else {
			// TODO: with OutwardIterator change, change 0, 0, 0, 0 to 0, 0, 0, -1
			liveObserverIterator.set(new WrappedSerizableIterator(new OutwardIterator(0, 0, 0, 0)));
		}
	}

	public <T extends Iterator<Vector3i> & Serializable> void setObserver(T custom) {
		if (custom == null) {
			setObserver(false);
		} else {
			entity.getData().put(IS_OBSERVER, true);
			liveObserverIterator.set(new WrappedSerizableIterator(custom));
		}
	}

	public Iterator<Vector3i> getSyncIterator() {
		WrappedSerizableIterator get = entity.getData().get(OBSERVER_ITERATOR);
		if (get != null) {
			return get.object;
		}
		Transform t = entity.getPhysics().getTransform();
		Point p = t.getPosition();
		int cx = p.getChunkX();
		int cy = p.getChunkY();
		int cz = p.getChunkZ();
		return new OutwardIterator(cx, cy, cz, getSyncDistance());
	}

	/**
	 * Gets the sync distance in {@link Chunk}s of the owning {@link org.spout.api.entity.Entity}. </p> Sync distance is a value indicating the radius outwards from the entity where network updates (such
	 * as chunk creation) will be triggered.
	 *
	 * @return The current sync distance
	 */
	public int getSyncDistance() {
		return entity.getData().get(SYNC_DISTANCE);
	}

	/**
	 * Sets the sync distance in {@link Chunk}s of the owning {@link org.spout.api.entity.Entity}.
	 *
	 * @param syncDistance The new sync distance
	 */
	public void setSyncDistance(final int syncDistance) {
		//TODO: Enforce server maximum (but that is set in Spout...)
		entity.getData().put(SYNC_DISTANCE, syncDistance);
	}

	private boolean first = true;

	/**
	 * Called when the owner is set to be synchronized to other NetworkComponents.
	 *
	 * TODO: Common logic between Spout and a plugin needing to implement this component? TODO: Add sequence checks to the PhysicsComponent to prevent updates to live?
	 *
	 * @param live A copy of the owner's live transform state
	 */
	public void finalizeRun(final Transform live) {
		if (!Spout.getPlatform().isServer()) {
			return;
		}
		//Entity changed chunks as observer OR observer status changed so update
		WrappedSerizableIterator old = entity.getData().get(OBSERVER_ITERATOR);
		Chunk snapChunk = entity.getPhysics().getPosition().getChunk(LoadOption.NO_LOAD);
		Chunk liveChunk = live.getPosition().getChunk(LoadOption.NO_LOAD);
		if (isObserver() && 
			((snapChunk == null ? liveChunk == null : snapChunk.equals(liveChunk))
				|| liveObserverIterator.get() != old
				|| old == INITIAL_TICK
				|| observeChunksFailed)) {
			updateObserver();
		}
	}

	/**
	 * Called just before a snapshot is taken of the owner.
	 *
	 * TODO: Add sequence checks to the PhysicsComponent to prevent updates to live?
	 *
	 * @param live A copy of the owner's live transform state
	 */
	public void preSnapshotRun(final Transform live) {
	}

	public void onDetached() {
		for (Chunk chunk : observingChunks) {
			// TODO: it shouldn't matter if the chunk is loaded?
			if (chunk.isLoaded()) {
				chunk.removeObserver(entity);
			}
		}
		observingChunks.clear();
	}

	protected void updateObserver() {
		first = false;
		final int syncDistance = getSyncDistance();
		World w = entity.getWorld();
		Transform t = entity.getPhysics().getTransform();
		Point p = t.getPosition();
		int cx = p.getChunkX();
		int cy = p.getChunkY();
		int cz = p.getChunkZ();
		Chunk center = p.getChunk(LOAD_GEN_NOWAIT);

		HashSet<Chunk> observing = new HashSet<>((syncDistance * syncDistance * syncDistance * 3) / 2);
		Iterator<Vector3i> itr = liveObserverIterator.get();
		if (itr == null) {
			itr = new OutwardIterator(cx, cy, cz, syncDistance);
		}
		observeChunksFailed = false;
		while (itr.hasNext()) {
			Vector3i v = itr.next();
			Chunk chunk = center == null ? w.getChunk(v.getX(), v.getY(), v.getZ(), LOAD_GEN_NOWAIT) : center.getRelative(v.getX() - cx, v.getY() - cy, v.getZ() - cz, LOAD_GEN_NOWAIT);
			if (chunk != null) {
				chunk.refreshObserver(entity);
				observing.add(chunk);
			} else {
				observeChunksFailed = true;
			}
		}
		observingChunks.removeAll(observing);
		// For every chunk that we were observing but not anymore
		for (Chunk chunk : observingChunks) {
			if (!chunk.isLoaded()) {
				throw new IllegalStateException("Chunk(" + chunk.getX() + " " + chunk.getY() + " " + chunk.getZ() + ") was unloaded while being observed!");
			}
			chunk.removeObserver(entity);
		}
		observingChunks.clear();
		observingChunks.addAll(observing);
	}

	public void copySnapshot() {
		if (first) {
			return;
		}
		entity.getData().put(OBSERVER_ITERATOR, liveObserverIterator.get());
	}
}