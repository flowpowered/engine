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
package com.flowpowered.api.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.Engine;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.reference.ChunkReference;
import com.flowpowered.api.util.ChunkIterator;
import com.flowpowered.engine.util.OutwardIterator;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractObserver {
    public static final ChunkIterator NO_CHUNKS = new ChunkIterator() {
        private static final long serialVersionUID = 1L;
        @Override
        @SuppressWarnings("unchecked")
        public Iterator<Vector3i> getIteratorFor(int centerX, int centerY, int centerZ) {
            return Collections.EMPTY_SET.iterator();
        }
    };
    /**
     * Chunks currently being observed
     */
    protected final AtomicReference<Set<ChunkReference>> observingChunks = new AtomicReference<>(new HashSet<>());
    protected final AtomicReference<ChunkIterator> liveObserverIterator = new AtomicReference<>(NO_CHUNKS);
    protected final Engine engine;
    protected boolean observeChunksFailed = true;
    protected LoadOption loadOpt = LoadOption.LOAD_GEN_NOWAIT;
    protected boolean keepNewLoaded = true;

    public AbstractObserver(Engine engine) {
        this.engine = engine;
    }

    public class SyncDistanceChunkIterator implements ChunkIterator {
        private static final long serialVersionUID = 1L;
        @Override
        public Iterator<Vector3i> getIteratorFor(int centerX, int centerY, int centerZ) {
            return new OutwardIterator(centerX, centerY, centerZ, getSyncDistance());
        }
    }

    /**
     * Returns if the owning {@link com.flowpowered.api.entity.Entity} is an observer. <p/> Observer means the Entity can trigger network updates (such as chunk creation) within its sync distance.
     *
     * @return True if observer, false if not
     */
    public abstract boolean isObserver();

    /**
     * Sets the observer status for the owning {@link com.flowpowered.api.entity.Entity}.
     * <br><br>
     * Passing <@code true> will cause the observer iterator to use the default iterator (sync distance), even if there was a previous custom iterator being used.
     *
     * @param observer True if observer, false if not
     */
    public void setObserver(final boolean observer) {
        setObserver(observer, observer ? new SyncDistanceChunkIterator() : NO_CHUNKS);
    }

    /**
     * Passing null will cause observer status to be set to false.
     */
    public void setObserver(ChunkIterator it) {
        setObserver(it != null, it == null ? NO_CHUNKS : it);
    }

    protected abstract void setObserver(boolean observer, ChunkIterator chunkIt);

    /**
     * Gets the sync distance in {@link Chunk}s of the owning {@link com.flowpowered.api.entity.Entity}. </p> Sync distance is a value indicating the radius outwards from the entity where network updates (such
     * as chunk creation) will be triggered.
     *
     * @return The current sync distance
     */
    public abstract int getSyncDistance();

    /**
     * Sets the sync distance in {@link Chunk}s of this observer.
     *
     * @param syncDistance The new sync distance
     */
    public abstract void setSyncDistance(final int syncDistance);

    public LoadOption getLoadOption() {
        return loadOpt;
    }

    public void setLoadOption(LoadOption loadOption) {
        this.loadOpt = loadOption;
    }

    public abstract void update();

    protected abstract Transform getTransform();

    public void onDetached() {
        Set<ChunkReference> observed = observingChunks.getAndSet(null);
        for (ChunkReference chunk : observed) {
            chunk.get().removeObserver(this);
        }
    }

    protected void updateObserver() {
        Transform t = getTransform();
        Set<ChunkReference> old = observingChunks.get();
        boolean newObserve = false;
        ImmutableSet.Builder<ChunkReference> newObserving = ImmutableSet.builder();
        HashSet<ChunkReference> observing = new HashSet<>();
        if (t != Transform.INVALID) {
            Point p = t.getPosition();
            World w = p.getWorld().refresh(engine.getWorldManager());
            final int cx = p.getChunkX();
            final int cy = p.getChunkY();
            final int cz = p.getChunkZ();
            Iterator<Vector3i> itr = liveObserverIterator.get().getIteratorFor(cx, cy, cz);
            Chunk center = w.getChunk(cx, cy, cz, loadOpt);
            observeChunksFailed = center == null;
            // TODO: fix this
            center = null;
            while (itr.hasNext()) {
                Vector3i v = itr.next();
                // We want to use relative when we can, it's faster
                Chunk chunk = center == null ? w.getChunk(v.getX(), v.getY(), v.getZ(), loadOpt) : center.getRelative(v.getX() - cx, v.getY() - cy, v.getZ() - cz, loadOpt);
                if (chunk != null) {
                    chunk.refreshObserver(this);
                    ChunkReference ref = new ChunkReference(chunk);
                    observing.add(ref);
                    if (!old.contains(ref)) {
                        newObserving.add(ref);
                        newObserve = true;
                    }
                } else {
                    observeChunksFailed = true;
                }
            }
            old.removeAll(observing);
        }
        // For every chunk that we were observing but not anymore
        for (ChunkReference ref : old) {
            Chunk chunk = ref.get();
            if (chunk == null) {
                continue;
            }
            chunk.removeObserver(this);
        }
        if (!old.isEmpty()) {
            stopObserving(ImmutableSet.copyOf(old));
        }
        if (newObserve) {
            startObserving(newObserving.build());
        }
        observingChunks.set(observing);
    }

    public Set<ChunkReference> getObservingChunks() {
        return observingChunks.get();
    }

    public abstract void copySnapshot();

    protected abstract void startObserving(ImmutableSet<ChunkReference> observing);

    protected abstract void stopObserving(ImmutableSet<ChunkReference> observing);
}
