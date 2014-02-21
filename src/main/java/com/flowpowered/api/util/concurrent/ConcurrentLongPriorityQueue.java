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

import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.flowpowered.math.GenericMath;

public class ConcurrentLongPriorityQueue<T extends LongPrioritized> {
    private final long keyMask;
    private final long keyStep;
    // We need to use RedirectableConcurrentLinkedQueues because if the queue is taken in addRaw, but then removed in complete, we don't want the added element in addRaw to get missed
    protected final ConcurrentSkipListMap<Long, RedirectableConcurrentLinkedQueue<T>> queueMap = new ConcurrentSkipListMap<>();

    public ConcurrentLongPriorityQueue(long resolution) {
        if (resolution < 1) {
            resolution = 1;
        }
        long mask = GenericMath.roundUpPow2(resolution);
        while (mask > resolution) {
            mask >>= 1;
        }
        this.keyMask = ~(mask - 1);
        this.keyStep = mask;
    }

    /**
     * Adds a prioritized element to the queue
     */
    public boolean add(T o) {
        return addRaw(o);
    }

    /**
     * This method is used to add elements to the queue when redirecting.  It should not be used for other purposes.
     */
    public boolean redirect(T o) {
        return addRaw(o);
    }

    private boolean addRaw(T o) {
        Long key = getKey(o.getPriority());
        RedirectableConcurrentLinkedQueue<T> queue = queueMap.get(key);
        if (queue == null) {
            queue = new RedirectableConcurrentLinkedQueue<>(key);
            RedirectableConcurrentLinkedQueue<T> previous = queueMap.putIfAbsent(key, queue);
            if (previous != null) {
                queue = previous;
            }
        }
        return queue.add(o);
    }

    /**
     * Removes a prioritized element from the queue
     */
    public boolean remove(T o) {
        Long key = getKey(o.getPriority());
        RedirectableConcurrentLinkedQueue<T> queue = queueMap.get(key);
        if (queue == null) {
            return false;
        }
        return queue.remove(o);
    }

    /**
     * Polls the queue for entries with a priority before or equal to the given threshold.<br> The sub-queue returned may have some entries that occur after the threshold and may not include all entries
     * that occur before the threshold.  The method returns null if there are no sub-queues before the threshold
     */
    public Queue<T> poll(long threshold) {
        Entry<Long, RedirectableConcurrentLinkedQueue<T>> first = queueMap.firstEntry();
        if (first == null || first.getKey() > threshold) {
            return null;
        } else {
            return first.getValue();
        }
    }

    /**
     * This method must be called for every sub-queue that is returned by the poll method.
     *
     * @param queue the queue that is returned
     * @return true if the threshold was covered by this sub-queue, so no further calls to poll() are required
     */
    public boolean complete(Queue<T> queue, long threshold) {
        RedirectableConcurrentLinkedQueue<T> q = (RedirectableConcurrentLinkedQueue<T>) queue;
        boolean empty = q.isEmpty();
        if (empty) {
            queueMap.remove(q.getPriority(), q);
            q.setRedirect(this);
            q.dumpToRedirect(this);
        }
        return q.getPriority() + keyStep > threshold;
    }

    /**
     * Returns true if the given queue is completely below the threshold
     */
    public boolean isFullyBelowThreshold(Queue<T> queue, long threshold) {
        RedirectableConcurrentLinkedQueue<T> q = (RedirectableConcurrentLinkedQueue<T>) queue;
        return q.getPriority() + keyStep <= threshold;
    }

    private Long getKey(long priority) {
        return priority & keyMask;
    }
}
