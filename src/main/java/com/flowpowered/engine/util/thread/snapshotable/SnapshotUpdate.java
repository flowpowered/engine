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

public class SnapshotUpdate<T> {
    private final boolean add;
    private final int index;
    private final T object;

    public SnapshotUpdate(T object, boolean add) {
        this.object = object;
        this.add = add;
        this.index = -1;
    }

    public SnapshotUpdate(int index, boolean add) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative indexs are not supported");
        } else if (add) {
            throw new IllegalArgumentException("An object must be provided when adding an object");
        }
        this.object = null;
        this.add = add;
        this.index = index;
    }

    public SnapshotUpdate(T object, int index, boolean add) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative indexs are not supported");
        } else if (!add) {
            throw new IllegalStateException("Removal of objects does not require both an index and an object");
        }
        this.object = object;
        this.add = add;
        this.index = index;
    }

    /**
     * Indicates if this update is an addition or removal
     *
     * @return true for additions
     */
    public boolean isAdd() {
        return add;
    }

    /**
     * Indicates if this is an indexed operation
     *
     * @return true for indexed operations
     */
    public boolean isIndexed() {
        return index >= 0;
    }

    /**
     * Gets the object
     *
     * @return the object
     */
    public T getObject() {
        return object;
    }

    /**
     * Gets the index
     */
    public int getIndex() {
        if (!isIndexed()) {
            throw new IllegalStateException("Cannot get the index of a non-indexed operation");
        }
        return index;
    }
}
