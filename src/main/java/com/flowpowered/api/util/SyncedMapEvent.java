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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.flowpowered.events.object.ObjectEvent;

/**
 * Event called when modifications occur on a StringMap
 */
public class SyncedMapEvent extends ObjectEvent<SyncedStringMap> {
    public static enum Action {
        ADD,
        SET,
        REMOVE,
    }

    private final Action action;
    private final List<Pair<Integer, String>> modifiedElements;

    public SyncedMapEvent(SyncedStringMap map, Action action, List<Pair<Integer, String>> modifiedElements) {
        super(map);
        this.action = action;
        this.modifiedElements = Collections.unmodifiableList(modifiedElements);
    }

    public Action getAction() {
        return action;
    }

    public List<Pair<Integer, String>> getModifiedElements() {
        return modifiedElements;
    }
}
