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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.flowpowered.events.object.EventableBase;

/**
 * Represents a map for mapping Strings to unique ids.
 *
 * The class supports conversion of ids between maps and allocation of new unique ids for unknown Strings
 *
 * Conversions to and from parent/child maps are cached
 */
public final class SyncedMapRegistry extends EventableBase<SyncedMapEvent> {
    public static final byte REGISTRATION_MAP = -1;
    protected static final SyncedStringMap STRING_MAP_REGISTRATION = new SyncedStringMap("REGISTRATION_MAP"); // This is a special case
    protected static final ConcurrentMap<String, WeakReference<SyncedStringMap>> REGISTERED_MAPS = new ConcurrentHashMap<>();

    public static SyncedStringMap get(String name) {
        WeakReference<SyncedStringMap> ref = REGISTERED_MAPS.get(name);
        if (ref != null) {
            SyncedStringMap map = ref.get();
            if (map == null) {
                REGISTERED_MAPS.remove(name);
            }
            return map;
        }
        return null;
    }

    public static SyncedStringMap get(int id) {
        if (id == REGISTRATION_MAP) {
            return STRING_MAP_REGISTRATION;
        }
        String name = STRING_MAP_REGISTRATION.getString(id);
        if (name != null) {
            WeakReference<SyncedStringMap> ref = REGISTERED_MAPS.get(name);
            if (ref != null) {
                SyncedStringMap map = ref.get();
                if (map == null) {
                    REGISTERED_MAPS.remove(name);
                }
                return map;
            }
        }
        return null;
    }

    public static Collection<SyncedStringMap> getAll() {
        Collection<WeakReference<SyncedStringMap>> rawMaps = REGISTERED_MAPS.values();
        List<SyncedStringMap> maps = new ArrayList<>(rawMaps.size());
        for (WeakReference<SyncedStringMap> ref : rawMaps) {
            SyncedStringMap map = ref.get();
            if (map != null) {
                maps.add(map);
            }
        }
        return maps;
    }

    public static SyncedStringMap getRegistrationMap() {
        return STRING_MAP_REGISTRATION;
    }

    public static int register(SyncedStringMap map) {
        int id = STRING_MAP_REGISTRATION.register(map.getName());
        REGISTERED_MAPS.put(map.getName(), new WeakReference<>(map));
        return id;
    }
}
