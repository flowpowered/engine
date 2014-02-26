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
package com.flowpowered.engine.util.thread.snapshotable;

import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.api.util.thread.annotation.SnapshotRead;

/**
 * A snapshotable array of type byte
 */
public class SnapshotableByteArray implements Snapshotable {
    private final byte[] snapshot;
    private final byte[] live;
    private final int[] dirtyArray;
    private final AtomicInteger dirtyIndex = new AtomicInteger(0);

    public SnapshotableByteArray(SnapshotManager manager, byte[] initial) {
        this(manager, initial, 100);
    }

    public SnapshotableByteArray(SnapshotManager manager, byte[] initial, int dirtySize) {
        snapshot = new byte[initial.length];
        live = new byte[initial.length];
        dirtyArray = new int[dirtySize];
        for (int i = 0; i < initial.length; i++) {
            snapshot[i] = initial[i];
            live[i] = initial[i];
        }
        if (manager != null) {
            manager.add(this);
        }
    }

    /**
     * Gets the snapshot value in the array
     *
     * @param index to lookup
     * @return snapshot value
     */
    @SnapshotRead
    public byte get(int index) {
        return snapshot[index];
    }

    /**
     * Gets the live value in the array
     *
     * @param index to lookup
     * @return live value
     */
    @LiveRead
    public byte getLive(int index) {
        synchronized (live) {
            return live[index];
        }
    }

    /**
     * Sets the value for the next snapshot
     *
     * @param index to set at
     * @param value to set to
     */
    @DelayedWrite
    public byte set(int index, byte value) {
        synchronized (live) {
            live[index] = value;
        }
        int localDirtyIndex = dirtyIndex.getAndIncrement();
        if (localDirtyIndex < dirtyArray.length) {
            dirtyArray[localDirtyIndex] = index;
        }
        return snapshot[index];
    }

    /**
     * Copies the next value to the snapshot value
     */
    @Override
    public void copySnapshot() {
        int length = dirtyIndex.get();
        if (length <= dirtyArray.length) {
            for (int i = 0; i < length; i++) {
                int index = dirtyArray[i];
                snapshot[index] = live[index];
            }
        } else {
            System.arraycopy(live, 0, snapshot, 0, live.length);
        }
    }
}
