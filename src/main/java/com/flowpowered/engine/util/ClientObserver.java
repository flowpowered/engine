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
package com.flowpowered.engine.util;

import com.flowpowered.api.Client;
import com.flowpowered.api.Engine;
import com.flowpowered.api.component.AbstractObserver;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.reference.ChunkReference;
import com.flowpowered.api.util.ChunkIterator;
import com.flowpowered.engine.scheduler.FlowScheduler;
import com.flowpowered.engine.scheduler.render.RenderThread;
import com.google.common.collect.ImmutableSet;

/**
 * This class "observes" chunks and sends them to the render or removes them from the renderer.
 */
public class ClientObserver extends AbstractObserver {
    private final Client client;

    public ClientObserver(Engine engine, Client client) {
        super(engine);
        this.client = client;
        liveObserverIterator.set(new SyncDistanceChunkIterator());
    }

    @Override
    public boolean isObserver() {
        return true;
    }

    @Override
    protected void setObserver(boolean observer, ChunkIterator chunkIt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSyncDistance() {
        return 10;
    }

    @Override
    public void setSyncDistance(final int syncDistance) {
    }

    @Override
    public void update() {
        updateObserver();
    }


    @Override
    public void copySnapshot() {
    }

    @Override
    protected Transform getTransform() {
        return client.getTransform();
    }

    @Override
    protected void startObserving(ImmutableSet<ChunkReference> observing) {
        RenderThread renderThread = ((FlowScheduler) engine.getScheduler()).getRenderThread();
        if (renderThread != null) {
            renderThread.addChunks(observing);
        }
    }

    @Override
    protected void stopObserving(ImmutableSet<ChunkReference> observing) {
        RenderThread renderThread = ((FlowScheduler) engine.getScheduler()).getRenderThread();
        if (renderThread != null) {
            renderThread.removeChunks(observing);
        }
    }
}