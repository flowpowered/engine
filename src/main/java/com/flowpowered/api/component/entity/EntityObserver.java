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
package com.flowpowered.api.component.entity;

import com.flowpowered.api.Server;
import com.flowpowered.api.component.AbstractObserver;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.event.EntityStartObservingChunksEvent;
import com.flowpowered.api.event.EntityStopObservingChunksEvent;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.reference.ChunkReference;
import com.flowpowered.api.util.ChunkIterator;
import com.flowpowered.commons.datatable.defaulted.DefaultedKey;
import com.flowpowered.commons.datatable.defaulted.DefaultedKeyImpl;
import com.google.common.collect.ImmutableSet;

/**
 * This class "observes" chunks and loads/gens them if necessary.
 */
public class EntityObserver extends AbstractObserver {
    public static final DefaultedKey<Boolean> IS_OBSERVER = new DefaultedKeyImpl<>("IS_OBSERVER", false);
    /**
     * null means use SYNC_DISTANCE_CHUNKS and is generated each update; not observing is {@code new OutwardIterator(0, 0, 0, -1)}; custom Iterators can be used for others We want default to be null so that when it
     * is default observer, it returns null
     */
    public static final DefaultedKey<ChunkIterator> OBSERVER_ITERATOR = new DefaultedKeyImpl<>("OBSERVER_ITERATOR", NO_CHUNKS);
    /**
     * In chunks
     */
    public static final DefaultedKey<Integer> SYNC_DISTANCE = new DefaultedKeyImpl<>("SYNC_DISTANCE", 10);
    private final Entity entity;

    public EntityObserver(Entity entity) {
        super(entity.getEngine());
        this.entity = entity;
    }

    @Override
    public boolean isObserver() {
        return entity.getData().get(IS_OBSERVER);
    }

    @Override
    protected void setObserver(boolean observer, ChunkIterator chunkIt) {
        entity.getData().put(IS_OBSERVER, observer);
        liveObserverIterator.set(chunkIt);
    }

    @Override
    public int getSyncDistance() {
        return entity.getData().get(SYNC_DISTANCE);
    }

    @Override
    public void setSyncDistance(final int syncDistance) {
        //TODO: Enforce server maximum (but that is set in Flow...)
        entity.getData().put(SYNC_DISTANCE, syncDistance);
    }

    @Override
    public void update() {
        if (engine.get(Server.class) == null) {
            return;
        }
        //Entity changed chunks as observer OR observer status changed so update
        ChunkIterator old = entity.getData().get(OBSERVER_ITERATOR);
        Chunk snapChunk = entity.getPhysics().getSnapshottedTransform().getPosition().getChunk(LoadOption.NO_LOAD, engine.getWorldManager());
        Chunk liveChunk = entity.getPhysics().getTransform().getPosition().getChunk(LoadOption.NO_LOAD, engine.getWorldManager());
        boolean needsUpdate = liveObserverIterator.get() != old || observeChunksFailed || (isObserver() && snapChunk == null ? liveChunk != null : !snapChunk.equals(liveChunk));
        if (needsUpdate) {
            updateObserver();
        }
    }


    @Override
    public void copySnapshot() {
        entity.getData().put(OBSERVER_ITERATOR, liveObserverIterator.get());
    }

    @Override
    protected Transform getTransform() {
        return entity.getPhysics().getTransform();
    }

    @Override
    protected void startObserving(ImmutableSet<ChunkReference> observing) {
        engine.getEventManager().callEvent(new EntityStartObservingChunksEvent(entity, observing));
    }

    @Override
    protected void stopObserving(ImmutableSet<ChunkReference> observing) {
        engine.getEventManager().callEvent(new EntityStopObservingChunksEvent(entity, observing));
    }
}