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
package com.flowpowered.engine.geo.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.flowpowered.commons.StringUtil;

import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldManager;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.util.thread.snapshotable.SnapshotableLinkedHashMap;

public class FlowWorldManager<T extends FlowWorld> implements WorldManager {
    protected final FlowEngine engine;
    protected final SnapshotableLinkedHashMap<String, T> loadedWorlds;

    public FlowWorldManager(FlowEngine engine) {
        loadedWorlds = new SnapshotableLinkedHashMap<>(engine.getSnapshotManager());
        this.engine = engine;
    }

    @Override
    public World getWorld(String name) {
        return getWorld(name, true);
    }

    @Override
    public World getWorld(String name, boolean exact) {
        if (exact) {
            FlowWorld world = loadedWorlds.get().get(name);
            if (world != null) {
                return world;
            }
            return loadedWorlds.get().get(name);
        } else {
            return StringUtil.getShortest(StringUtil.matchName(loadedWorlds.getValues(), name));
        }
    }

    @Override
    public Collection<World> matchWorld(String name) {
        return StringUtil.matchName(getWorlds(), name);
    }

    @Override
    public FlowWorld getWorld(UUID uid) {
        for (FlowWorld world : loadedWorlds.getValues()) {
            if (world.getUID().equals(uid)) {
                return world;
            }
        }
        return null;
    }

    @Override
    public Collection<World> getWorlds() {
        Collection<World> w = new ArrayList<>();
        for (FlowWorld world : loadedWorlds.getValues()) {
            w.add(world);
        }
        return w;
    }

}
