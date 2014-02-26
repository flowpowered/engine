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

import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.api.util.thread.annotation.SnapshotRead;

/**
 * A snapshotable object that supports primitive bytes
 */
public class SnapshotableByte implements Snapshotable {
    private volatile byte next;
    private byte snapshot;

    public SnapshotableByte(SnapshotManager manager, byte initial) {
        next = initial;
        snapshot = initial;
        if (manager != null) {
            manager.add(this);
        }
    }

    /**
     * Sets the next value for the Snapshotable
     */
    @DelayedWrite
    public void set(byte next) {
        this.next = next;
    }

    /**
     * Gets the snapshot value for
     *
     * @return the stable snapshot value
     */
    @SnapshotRead
    public byte get() {
        return snapshot;
    }

    /**
     * Gets the live value
     *
     * @return the unstable Live "next" value
     */
    @LiveRead
    public byte getLive() {
        return next;
    }

    /**
     * Copies the next value to the snapshot value
     */
    @Override
    public void copySnapshot() {
        snapshot = next;
    }
}
