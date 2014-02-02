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

import com.flowpowered.math.imaginary.Quaternionf;
import org.spout.nbt.FloatTag;
import org.spout.nbt.ListTag;
import org.spout.nbt.Tag;
import org.spout.nbt.util.NBTMapper;

public class QuaternionTag extends ListTag<FloatTag> {
    public QuaternionTag(String name, Quaternionf q) {
        super(name, FloatTag.class, quaternionToList(q));
    }

    private static List<FloatTag> quaternionToList(Quaternionf q) {
        List<FloatTag> list = new ArrayList<>(4);
        list.add(new FloatTag("", q.getX()));
        list.add(new FloatTag("", q.getY()));
        list.add(new FloatTag("", q.getZ()));
        list.add(new FloatTag("", q.getW()));
        return list;
    }

    @SuppressWarnings ("unchecked")
    public static Quaternionf getValue(Tag<?> tag) {
        try {
            return getValue((ListTag<FloatTag>) tag);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static Quaternionf getValue(ListTag<FloatTag> list) {
        if (list == null) {
            return null;
        }
        return getValue(list.getValue());
    }

    public static Quaternionf getValue(List<FloatTag> list) {
        if (list == null || list.size() != 4) {
            return null;
        }
        Float x = NBTMapper.toTagValue(list.get(0), Float.class, null);
        Float y = NBTMapper.toTagValue(list.get(1), Float.class, null);
        Float z = NBTMapper.toTagValue(list.get(2), Float.class, null);
        Float w = NBTMapper.toTagValue(list.get(3), Float.class, null);
        if (x == null || y == null || z == null || w == null) {
            return null;
        }
        return new Quaternionf(x, y, z, w);
    }
}
