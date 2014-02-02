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
package com.flowpowered.api.geo.cuboid.reference;

import java.lang.ref.WeakReference;

import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.geo.discrete.Point;

/**
 * This holds a {@code WeakReference<Region>} that can be used streamline the get() with a isLoaded check. It also adds a
 * store of a {@code Point} representing the base. Because of this, a RegionReference may contain only base info.
 */
public class RegionReference {
    private final Point base;
    private WeakReference<Region> region;
    public RegionReference(Region referent) {
        this.region = new WeakReference<>(referent);
        base = referent.getBase();
    }

    public RegionReference(Point base) {
        region = null;
        this.base = base;
    }

    public Region get() {
        Region get = region == null ? null : region.get();
        if (get != null) {
            if (!get.isLoaded()) {
                region = null;
                return null;
            }
        }
        return get;
    }

    public Region refresh(LoadOption opt) {
        Region newRegion = get();
        if (newRegion != null) return newRegion;
        newRegion = base.getRegion(opt);
        this.region = newRegion == null ? null : new WeakReference<>(newRegion);
        return newRegion;
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RegionReference) {
            return base.equals(((RegionReference) obj).base);
        }
        return false;
    }

    public Point getBase() {
        return base;
    }
}
