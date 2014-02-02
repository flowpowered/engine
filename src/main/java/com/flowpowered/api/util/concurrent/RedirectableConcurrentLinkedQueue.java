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
package com.flowpowered.api.util.concurrent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class RedirectableConcurrentLinkedQueue<T extends LongPrioritized> extends ConcurrentLinkedQueue<T> implements LongPrioritized {
    private static final long serialVersionUID = 1L;
    private final AtomicReference<ConcurrentLongPriorityQueue<T>> redirect = new AtomicReference<>();
    private final long priority;

    public RedirectableConcurrentLinkedQueue(long priority) {
        this.priority = priority;
    }

    @Override
    public boolean add(T e) {
        super.add(e);
        ConcurrentLongPriorityQueue<T> r = redirect.get();
        if (r != null) {
            dumpToRedirect(r);
        }
        return true;
    }

    public void dumpToRedirect(ConcurrentLongPriorityQueue<T> target) {
        T next;
        while ((next = poll()) != null) {
            target.redirect(next);
        }
    }

    public void setRedirect(ConcurrentLongPriorityQueue<T> target) {
        if (!redirect.compareAndSet(null, target)) {
            throw new IllegalStateException("Redirect may not be set more than once per redirectable queue");
        }
    }

    @Override
    public long getPriority() {
        return priority;
    }
}
