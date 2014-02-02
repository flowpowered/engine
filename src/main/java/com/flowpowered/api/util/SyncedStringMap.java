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
package com.flowpowered.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import com.flowpowered.commons.StringToUniqueIntegerMap;
import com.flowpowered.commons.store.SimpleStore;

import org.apache.commons.lang3.tuple.Pair;

import com.flowpowered.events.object.Eventable;
import com.flowpowered.events.object.EventableListener;

        
/**
 * This class syncs a StringToUniqueIntegerMap from server to client
 */
public final class SyncedStringMap extends StringToUniqueIntegerMap implements Eventable<SyncedMapEvent> {
    private final CopyOnWriteArrayList<EventableListener<SyncedMapEvent>> registeredListeners = new CopyOnWriteArrayList<>();
    private int id;

    protected SyncedStringMap(String name) {
        super(name);
    }

    protected SyncedStringMap(StringToUniqueIntegerMap parent, SimpleStore<Integer> store, int minId, int maxId, String name) {
        super(parent, store, minId, maxId, name);
    }

    public static SyncedStringMap create(String name) {
        SyncedStringMap map = new SyncedStringMap(name);
        map.id = SyncedMapRegistry.register(map);
        return map;
    }

    public static SyncedStringMap create(StringToUniqueIntegerMap parent, SimpleStore<Integer> store, int minId, int maxId, String name) {
        SyncedStringMap map = new SyncedStringMap(parent, store, minId, maxId, name);
        map.id = SyncedMapRegistry.register(map);
        return map;
    }

    @Override
    public int register(String key) {
        Integer id = store.get(key);
        if (id != null) {
            return id;
        }
        int local = super.register(key);
        callEvent(new SyncedMapEvent(this, SyncedMapEvent.Action.ADD, Arrays.asList(Pair.of(local, key))));
        return local;
    }

    @Override
    public boolean register(String key, int id) {
        Integer local = store.get(key);
        if (local != null) {
            return false;
        }
        callEvent(new SyncedMapEvent(this, SyncedMapEvent.Action.ADD, Arrays.asList(Pair.of(id, key))));
        return super.register(key, id);
    }

    public void handleUpdate(SyncedMapEvent message) {
        switch (message.getAction()) {
            case SET:
                super.clear();
            case ADD:
                for (Pair<Integer, String> pair : message.getModifiedElements()) {
                    store.set(pair.getValue(), pair.getKey());
                }
                break;
            case REMOVE:
                for (Pair<Integer, String> pair : message.getModifiedElements()) {
                    store.remove(pair.getValue());
                }
                break;
        }
        callEvent(new SyncedMapEvent(this, message.getAction(), message.getModifiedElements()));
    }

    @Override
    public void clear() {
        super.clear();
        callEvent(new SyncedMapEvent(this, SyncedMapEvent.Action.SET, new ArrayList<Pair<Integer, String>>()));
    }

    public int getId() {
        return id;
    }

    @Override
    public void registerListener(EventableListener<SyncedMapEvent> listener) {
        registeredListeners.add(listener);
    }

    @Override
    public void unregisterAllListeners() {
        registeredListeners.clear();
    }

    @Override
    public void unregisterListener(EventableListener<SyncedMapEvent> listener) {
        registeredListeners.remove(listener);
    }

    @Override
    public void callEvent(SyncedMapEvent event) {
        for (EventableListener<SyncedMapEvent> listener : registeredListeners) {
            listener.onEvent(event);
        }
    }
}
