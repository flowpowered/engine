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
package com.flowpowered.api.geo.reference;

import java.lang.ref.WeakReference;

import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldManager;

/**
 * This holds a {@code WeakReference<World>} that can be used streamline the get() with a isLoaded check. It also adds a
 * store of a {@code String} representing the name. Because of this, a WorldReference may contain only the name.
 */
public class WorldReference {
    private String name;
    private WeakReference<World> world;

    public WorldReference(World referent) {
        this.world = new WeakReference<>(referent);
        this.name = referent.getName();
    }

    public WorldReference(String name) {
        world = null;
        this.name = name;
    }

    public WorldReference(WorldReference reference) {
        this.name = reference.name;
        World get = reference.world.get();
        this.world = get == null ? null : new WeakReference<>(get);
    }

    public World get() {
        World get = world == null ? null : world.get();
        if (get != null) {
            if (!get.isLoaded()) {
                world = null;
                return null;
            }
        }
        return get;
    }

    public World refresh(WorldManager manager) {
        World newWorld = get();
        if (newWorld != null) return newWorld;
        newWorld = manager.getWorld(name);
        this.world = newWorld == null ? null : new WeakReference<>(newWorld);
        return newWorld;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WorldReference) {
            return name.equals(((WorldReference) obj).name);
        }
        return false;
    }

    public String getName() {
        return name;
    }
}
