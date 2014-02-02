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

import com.flowpowered.engine.util.OutwardIterator;
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

import com.flowpowered.api.Flow;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.math.vector.Vector3i;

/**
 * The networking behind {@link com.flowpowered.api.entity.Entity}s.
 */
public class Network {
    public static final LoadOption LOAD_GEN_NOWAIT = new LoadOption(true, true, false);
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
    private boolean observeChunksFailed = true;
    private final Entity entity;

    public Network(Entity entity) {
        this.entity = entity;
        setObserver(true);
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
     * Returns if the owning {@link com.flowpowered.api.entity.Entity} is an observer. <p/> Observer means the Entity can trigger network updates (such as chunk creation) within its sync distance.
     *
     * @return True if observer, false if not
     */
    public boolean isObserver() {
        return entity.getData().get(IS_OBSERVER);
    }

    /**
     * Sets the observer status for the owning {@link com.flowpowered.api.entity.Entity}. If there was a custom observer iterator being used, passing {@code true} will cause it to reset to the default observer
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
        Transform t = entity.getPhysics().getSnapshottedTransform();
        Point p = t.getPosition();
        int cx = p.getChunkX();
        int cy = p.getChunkY();
        int cz = p.getChunkZ();
        return new OutwardIterator(cx, cy, cz, getSyncDistance());
    }

    /**
     * Gets the sync distance in {@link Chunk}s of the owning {@link com.flowpowered.api.entity.Entity}. </p> Sync distance is a value indicating the radius outwards from the entity where network updates (such
     * as chunk creation) will be triggered.
     *
     * @return The current sync distance
     */
    public int getSyncDistance() {
        return entity.getData().get(SYNC_DISTANCE);
    }

    /**
     * Sets the sync distance in {@link Chunk}s of the owning {@link com.flowpowered.api.entity.Entity}.
     *
     * @param syncDistance The new sync distance
     */
    public void setSyncDistance(final int syncDistance) {
        //TODO: Enforce server maximum (but that is set in Flow...)
        entity.getData().put(SYNC_DISTANCE, syncDistance);
    }

    private boolean first = true;

    /**
     * Called when the owner is set to be synchronized to other NetworkComponents.
     *
     * TODO: Common logic between Flow and a plugin needing to implement this component? TODO: Add sequence checks to the PhysicsComponent to prevent updates to live?
     *
     * @param live A copy of the owner's live transform state
     */
    public void finalizeRun(final Transform live) {
        if (!entity.getWorld().getEngine().getPlatform().isServer()) {
            return;
        }
        //Entity changed chunks as observer OR observer status changed so update
        WrappedSerizableIterator old = entity.getData().get(OBSERVER_ITERATOR);
        Chunk snapChunk = entity.getPhysics().getPosition().getChunk(LoadOption.NO_LOAD);
        Chunk liveChunk = live.getPosition().getChunk(LoadOption.NO_LOAD);
        if (isObserver() && 
            ((snapChunk == null ? liveChunk != null : !snapChunk.equals(liveChunk))
                || liveObserverIterator.get() != old
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
        Transform t = entity.getPhysics().getSnapshottedTransform();
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
                throw new IllegalStateException("Chunk(" + chunk.getChunkX() + " " + chunk.getChunkY() + " " + chunk.getChunkZ() + ") was unloaded while being observed!");
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