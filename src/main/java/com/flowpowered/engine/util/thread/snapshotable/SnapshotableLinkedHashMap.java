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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.api.util.thread.annotation.SnapshotRead;

/**
 * A snapshotable class for LinkedHashMaps
 */
public class SnapshotableLinkedHashMap<K, V> implements Snapshotable {
    private final Map<K, V> snapshot = new LinkedHashMap<>();
    private final Map<K, V> unmodifySnapshot = Collections.unmodifiableMap(snapshot);
    private final Collection<V> unmodifyValues = Collections.unmodifiableCollection(snapshot.values());
    private final ConcurrentMap<K, V> live = new ConcurrentHashMap<>();
    private final Map<K, V> unmodifyLive = Collections.unmodifiableMap(live);
    private final ConcurrentLinkedQueue<K> dirty = new ConcurrentLinkedQueue<>();
    private final ArrayList<K> dirtyList = new ArrayList<>();
    private final HashSet<K> dirtyListTemp = new HashSet<>();
    private final List<K> unmodifyDirty = Collections.unmodifiableList(dirtyList);
    private boolean dirtyListGenerated = false;

    public SnapshotableLinkedHashMap(SnapshotManager manager) {
        manager.add(this);
    }

    /**
     * Adds a key/value pair to the map
     *
     * @param key the key
     * @param value the value
     * @return the old value
     */
    @DelayedWrite
    @LiveRead
    public V put(K key, V value) {
        V oldValue = live.put(key, value);
        dirty.add(key);
        return oldValue;
    }

    /**
     * Adds a key/value pair to the map, if no value exists for the key
     *
     * @param key the key
     * @param value the value
     * @return the old value
     */
    @DelayedWrite
    @LiveRead
    public V putIfAbsent(K key, V value) {
        V oldValue = live.putIfAbsent(key, value);
        if (oldValue == null) {
            dirty.add(key);
        }
        return oldValue;
    }

    /**
     * Removes a key/value pair from the list
     *
     * @param key the key
     * @return the old value
     */
    @DelayedWrite
    @LiveRead
    public V remove(K key) {
        V oldValue = live.remove(key);
        if (oldValue != null) {
            dirty.add(key);
        }
        return oldValue;
    }

    /**
     * Removes a key/value pair from the list
     *
     * @param key the key
     * @param value the value
     * @return true if the key/value pair was removed
     */
    @DelayedWrite
    @LiveRead
    public boolean remove(K key, V value) {
        boolean success = live.remove(key, value);
        if (success) {
            dirty.add(key);
        }
        return success;
    }

    /**
     * Gets the snapshot value
     *
     * @return the stable snapshot value
     */
    @SnapshotRead
    public Map<K, V> get() {
        return unmodifySnapshot;
    }

    /**
     * Gets the live value
     *
     * @return the live set
     */
    @LiveRead
    public Map<K, V> getLive() {
        return unmodifyLive;
    }

    /**
     * Get the values in the map, in order
     *
     * @return the values
     */
    @SnapshotRead
    public Collection<V> getValues() {
        return unmodifyValues;
    }

    /**
     * Get the values in the map, in order
     *
     * @return the values
     */
    @SnapshotRead
    public Collection<V> getValuesLive() {
        throw new UnsupportedOperationException("Iterating over the live values is not possible, since they are not stored LinkedHashSet");
    }

    /**
     * Creates a list of keys that have been changed since the last snapshot copy.<br> <br> This method may only be called during the pre-snapshot stage and the list only remains valid during that
     * stage.
     *
     * @return the list of elements that have been updated
     */
    public List<K> getDirtyList() {
        TickStage.checkStage(TickStage.PRESNAPSHOT);
        if (!dirtyListGenerated) {
            for (K o : dirty) {
                if (dirtyListTemp.add(o)) {
                    dirtyList.add(o);
                }
            }
            dirtyListTemp.clear();
            dirtyListGenerated = true;
        }
        return unmodifyDirty;
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
        for (K key : dirty) {
            V value = live.get(key);
            if (value == null) {
                snapshot.remove(key);
            } else {
                snapshot.put(key, value);
            }
        }
        dirty.clear();
        dirtyList.clear();
        dirtyListGenerated = false;
    }
}
