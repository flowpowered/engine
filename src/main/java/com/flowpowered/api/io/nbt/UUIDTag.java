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
import java.util.UUID;

import org.spout.nbt.ListTag;
import org.spout.nbt.LongTag;
import org.spout.nbt.Tag;
import org.spout.nbt.util.NBTMapper;

public class UUIDTag extends ListTag<LongTag> {
    public UUIDTag(String name, UUID u) {
        super(name, LongTag.class, UUIDToList(u));
    }

    private static List<LongTag> UUIDToList(UUID u) {
        List<LongTag> list = new ArrayList<>(2);
        list.add(new LongTag("", u.getMostSignificantBits()));
        list.add(new LongTag("", u.getLeastSignificantBits()));
        return list;
    }

    @SuppressWarnings ("unchecked")
    public static UUID getValue(Tag<?> tag) {
        try {
            return getValue((ListTag<LongTag>) tag);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static UUID getValue(ListTag<LongTag> list) {
        if (list == null) {
            return null;
        }
        return getValue(list.getValue());
    }

    public static UUID getValue(List<LongTag> list) {
        if (list == null || list.size() != 2) {
            return null;
        }
        Long m = NBTMapper.toTagValue(list.get(0), Long.class, null);
        Long l = NBTMapper.toTagValue(list.get(1), Long.class, null);

        if (m == null || l == null) {
            return null;
        }
        return new UUID(m, l);
    }
}
