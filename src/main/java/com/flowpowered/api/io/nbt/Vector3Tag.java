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
package com.flowpowered.api.io.nbt;

import java.util.ArrayList;
import java.util.List;

import com.flowpowered.math.vector.Vector3f;
import org.spout.nbt.FloatTag;
import org.spout.nbt.ListTag;
import org.spout.nbt.Tag;
import org.spout.nbt.util.NBTMapper;

public class Vector3Tag extends ListTag<FloatTag> {
    public Vector3Tag(String name, Vector3f v) {
        super(name, FloatTag.class, vector3ToList(v));
    }

    private static List<FloatTag> vector3ToList(Vector3f v) {
        List<FloatTag> list = new ArrayList<>(3);
        list.add(new FloatTag("", v.getX()));
        list.add(new FloatTag("", v.getY()));
        list.add(new FloatTag("", v.getZ()));
        return list;
    }

    @SuppressWarnings ("unchecked")
    public static Vector3f getValue(Tag<?> tag) {
        try {
            return getValue((ListTag<FloatTag>) tag);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static Vector3f getValue(ListTag<FloatTag> list) {
        if (list == null) {
            return null;
        }
        return getValue(list.getValue());
    }

    public static Vector3f getValue(List<FloatTag> list) {
        if (list == null || list.size() != 3) {
            return null;
        }
        Float x = NBTMapper.toTagValue(list.get(0), Float.class, null);
        Float y = NBTMapper.toTagValue(list.get(1), Float.class, null);
        Float z = NBTMapper.toTagValue(list.get(2), Float.class, null);

        if (x == null || y == null || z == null) {
            return null;
        }

        return new Vector3f(x, y, z);
    }
}
