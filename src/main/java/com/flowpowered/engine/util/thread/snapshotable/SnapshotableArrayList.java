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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.api.util.thread.annotation.SnapshotRead;

/**
 * A snapshotable object for ArrayLists
 */
public class SnapshotableArrayList<T> implements Snapshotable {
    private final ConcurrentLinkedQueue<T> dirty = new ConcurrentLinkedQueue<>();
    private final List<T> snapshot;
    private final List<T> live;

    public SnapshotableArrayList(SnapshotManager manager) {
        this(manager, null);
    }

    public SnapshotableArrayList(SnapshotManager manager, ArrayList<T> initial) {
        if (initial != null) {
            snapshot = new ArrayList<>(initial);
        } else {
            snapshot = new ArrayList<>();
        }
        live = Collections.synchronizedList(new ArrayList<>(snapshot));
        if (manager != null) {
            manager.add(this);
        }
    }

    /**
     * Adds an object to the list
     */
    @DelayedWrite
    public boolean add(T object) {
        boolean success = live.add(object);

        if (success) {
            dirty.add(object);
        }

        return success;
    }

    @DelayedWrite
    public void addAll(Collection<T> values) {
        for (T object : values) {
            boolean success = live.add(object);

            if (success) {
                dirty.add(object);
            }
        }
    }

    /**
     * Removes an object from the list
     */
    @DelayedWrite
    public boolean remove(T object) {
        boolean success = live.remove(object);

        if (success) {
            dirty.add(object);
        }

        return success;
    }

    /**
     * Removes the object from the list at a particular index
     */
    @DelayedWrite
    public void remove(int index) {
        dirty.add(live.remove(index));
    }

    /**
     * Gets the snapshot value
     *
     * @return the stable snapshot value
     */
    @SnapshotRead
    public List<T> get() {
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Gets the live value
     *
     * @return the live value
     */
    @LiveRead
    public List<T> getLive() {
        return Collections.unmodifiableList(live);
    }

    /**
     * Gets the dirty object list
     *
     * @return the dirty list
     */
    @LiveRead
    public List<T> getDirtyList() {
        return Collections.unmodifiableList(new ArrayList<>(dirty));
    }

    /**
     * Copies the next values to the snapshot
     */
    @Override
    public void copySnapshot() {
        if (dirty.size() > 0) {
            snapshot.clear();
            synchronized (live) {
                for (T o : live) {
                    snapshot.add(o);
                }
            }
        }
        dirty.clear();
    }
}
