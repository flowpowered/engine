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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.api.util.thread.annotation.SnapshotRead;

/**
 * A snapshotable class for HashSets
 */
public class SnapshotableHashSet<T> implements Snapshotable {
    private final Set<T> snapshot = new HashSet<>();
    private final Set<T> unmodifySnapshot = Collections.unmodifiableSet(snapshot);
    private final Set<T> live = Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
    private final Set<T> unmodifyLive = Collections.unmodifiableSet(live);
    private final ConcurrentLinkedQueue<T> dirty = new ConcurrentLinkedQueue<>();
    private final ArrayList<T> dirtyList = new ArrayList<>();

    public SnapshotableHashSet(SnapshotManager manager) {
        this(manager, null);
    }

    public SnapshotableHashSet(SnapshotManager manager, HashSet<T> initial) {
        if (initial != null) {
            for (T o : initial) {
                add(o);
            }
        }
        manager.add(this);
    }

    /**
     * Adds an object to the list
     *
     * @return true if the object was successfully added
     */
    @DelayedWrite
    @LiveRead
    public boolean add(T object) {
        boolean success = live.add(object);
        if (success) {
            dirty.add(object);
        }
        return success;
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
     * Gets the snapshot value
     *
     * @return the stable snapshot value
     */
    @SnapshotRead
    public Set<T> get() {
        return unmodifySnapshot;
    }

    /**
     * Gets the live value
     *
     * @return the live set
     */
    public Set<T> getLive() {
        return unmodifyLive;
    }

    /**
     * Creates a list of elements that have been changed since the last snapshot copy.<br> <br> This method may only be called during the pre-snapshot stage and the list only remains valid during that
     * stage.
     *
     * @return the list of elements that have been updated
     */
    public List<T> getDirtyList() {
        TickStage.checkStage(TickStage.PRESNAPSHOT);
        return Collections.unmodifiableList(new ArrayList<>(dirty));
    }

    /**
     * Tests if the set is empty
     *
     * @return true if the set is empty
     */
    public boolean isEmptyLive() {
        return live.isEmpty();
    }

    /**
     * Copies the next values to the snapshot
     */
    @Override
    public void copySnapshot() {
        for (T o : dirty) {
            if (live.contains(o)) {
                snapshot.add(o);
            } else {
                snapshot.remove(o);
            }
        }
        dirty.clear();
        dirtyList.clear();
    }
}
