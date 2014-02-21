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
package com.flowpowered.engine.scheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import com.flowpowered.api.util.concurrent.ConcurrentLongPriorityQueue;
import com.flowpowered.api.util.concurrent.RedirectableConcurrentLinkedQueue;
import com.flowpowered.engine.util.thread.AsyncManager;

public class TaskPriorityQueue extends ConcurrentLongPriorityQueue<FlowTask> {
    private final AsyncManager taskManager;

    public TaskPriorityQueue(AsyncManager manager, long resolution) {
        super(resolution);
        taskManager = manager;
    }

    /**
     * Gets the first pending task on the queue.  A task is considered pending if its next call time is less than or equal to the given current time.<br> <br> NOTE: This method should only be called from
     * a single thread.
     *
     * @param currentTime the current time
     * @return the first pending task, or null if no task is pending
     */
    public Queue<FlowTask> getPendingTask(long currentTime) {
        if (taskManager != null && Thread.currentThread() != taskManager.getExecutionThread()) {
            throw new IllegalStateException("getPendingTask() may only be called from the thread that created the TaskPriorityQueue");
        }

        return super.poll(currentTime);
    }

    @Override
    public boolean add(FlowTask task) {
        if (task != null) {
            if (task.isDone()) {
                throw new UnsupportedOperationException("Task was dead when adding to the queue");
            }
        }
        return super.add(task);
    }

    @Override
    public boolean redirect(FlowTask task) {
        return super.add(task);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (FlowTask t : getTasks()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("{").append(t.getTaskId()).append(":").append(t.getNextCallTime()).append("}");
        }
        return sb.append("}").toString();
    }

    public List<FlowTask> getTasks() {
        List<FlowTask> list = new ArrayList<>();
        Iterator<RedirectableConcurrentLinkedQueue<FlowTask>> iq = queueMap.values().iterator();
        while (iq.hasNext()) {
            Iterator<FlowTask> i = iq.next().iterator();
            while (i.hasNext()) {
                FlowTask t = i.next();
                list.add(t);
            }
        }
        return list;
    }
}

