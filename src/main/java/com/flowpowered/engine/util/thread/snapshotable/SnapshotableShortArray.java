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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.api.util.thread.annotation.SnapshotRead;

/**
 * A snapshotable array of type short
 */
public class SnapshotableShortArray implements Snapshotable {
    private final short[] snapshot;
    private final AtomicIntegerArray live;
    private final AtomicIntegerArray dirtyArray;
    private final int dirtySize;
    private final AtomicInteger dirtyIndex = new AtomicInteger(0);

    public SnapshotableShortArray(SnapshotManager manager, short[] initial) {
        this(manager, initial, 50);
    }

    public SnapshotableShortArray(SnapshotManager manager, short[] initial, int dirtySize) {
        snapshot = new short[initial.length];
        live = new AtomicIntegerArray(initial.length >> 1);
        this.dirtySize = dirtySize;
        dirtyArray = new AtomicIntegerArray(dirtySize);
        for (int i = 0; i < initial.length; i++) {
            snapshot[i] = initial[i];
            set(i, initial[i]);
        }
        if (manager != null) {
            manager.add(this);
        }
    }

    /**
     * Gets a copy of the snapshot short array
     *
     * @return copy of the snapshot short array
     */
    public short[] get() {
        return Arrays.copyOf(snapshot, snapshot.length);
    }

    /**
     * Gets a copy of the live short array
     *
     * @return copy of the live short array
     */
    public short[] getLive() {
        short[] live = new short[snapshot.length];
        for (int i = 0; i < this.live.length(); i++) {
            int value = this.live.get(i);
            live[(i << 1)] = (short) (value & 0xFFFF);
            live[(i << 1) + 1] = (short) (value >> 16 & 0xFFFF);
        }
        return live;
    }

    /**
     * Gets the snapshot value in the array
     *
     * @param index to lookup
     * @return snapshot value
     */
    @SnapshotRead
    public short get(int index) {
        return snapshot[index];
    }

    /**
     * Gets the live value in the array
     *
     * @param index to lookup
     * @return live value
     */
    @LiveRead
    public short getLive(int index) {
        int packed = live.get(index >> 1);
        if ((index & 0x1) == 0) {
            return unpackZero(packed);
        } else {
            return unpackOne(packed);
        }
    }

    /**
     * Sets the value for the next snapshot
     *
     * @param index to set at
     * @param value to set to
     * @return the old value
     */
    @DelayedWrite
    public short set(int index, short value) {
        boolean success = false;
        int divIndex = index >> 1;
        boolean isZero = (index & 0x1) == 0;
        short one;
        short zero;
        short old = 0;

        while (!success) {
            int packed = live.get(divIndex);
            if (isZero) {
                old = unpackZero(packed);
                one = unpackOne(packed);
                zero = value;
            } else {
                old = unpackOne(packed);
                one = value;
                zero = unpackZero(packed);
            }
            success = live.compareAndSet(divIndex, packed, pack(zero, one));
        }
        markDirty(index);
        return old;
    }

    private void markDirty(int index) {
        int localDirtyIndex = dirtyIndex.getAndIncrement();
        if (localDirtyIndex < dirtySize) {
            dirtyArray.set(localDirtyIndex, index);
        }
    }

    /**
     * Copies the next value to the snapshot value
     */
    @Override
    public void copySnapshot() {
        int length = dirtyIndex.get();
        if (length <= dirtySize) {
            for (int i = 0; i < length; i++) {
                int index = dirtyArray.get(i);
                snapshot[index] = getLive(i);
            }
        } else {
            for (int i = 0; i < snapshot.length; i++) {
                snapshot[i] = getLive(i);
            }
        }
    }

    private int pack(short zero, short one) {
        return (one & 0xFFFF) << 16 | zero & 0xFFFF;
    }

    private short unpackZero(int value) {
        return (short) value;
    }

    private short unpackOne(int value) {
        return (short) (value >> 16);
    }
}
