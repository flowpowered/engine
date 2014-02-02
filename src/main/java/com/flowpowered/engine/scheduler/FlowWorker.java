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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.scheduler.Worker;
import com.flowpowered.commons.future.SimpleFuture;

public class FlowWorker implements Worker, Runnable {
    @SuppressWarnings ("rawtypes")
    private static final Future<?> NOT_SUBMITED = new SimpleFuture();
    @SuppressWarnings ("rawtypes")
    private static final Future<?> CANCELLED = new SimpleFuture();
    private final int id;
    private final Object owner;
    private final FlowTask task;
    private final Thread thread;
    private final Runnable r;
    private AtomicReference<Future<?>> futureRef = new AtomicReference<Future<?>>(NOT_SUBMITED);
    private boolean shouldContinue = true;
    private final FlowTaskManager taskManager;

    protected FlowWorker(final FlowTask task, final FlowTaskManager taskManager) {
        id = task.getTaskId();
        owner = task.getOwner();
        this.task = task;
        String name = "Flow Worker{Owner:" + ((owner != null) ? owner.getClass().getName() : "none") + ", id:" + id + "}";
        r = new Runnable() {
            @Override
            public void run() {
                task.pulse();
                taskManager.removeWorker(FlowWorker.this, task);
                taskManager.repeatSchedule(task);
            }
        };
        if (task.isLongLived()) {
            thread = new Thread(r, name);
        } else {
            thread = null;
        }
        this.taskManager = taskManager;
    }

    public void start(ExecutorService pool) {
        if (thread != null) {
            thread.start();
        } else {
            Future<?> future = pool.submit(r);
            if (!this.futureRef.compareAndSet(NOT_SUBMITED, future)) {
                future.cancel(true);
            }
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int getTaskId() {
        return id;
    }

    @Override
    public Object getOwner() {
        return owner;
    }

    @Override
    public FlowTask getTask() {
        return task;
    }

    public boolean shouldContinue() {
        return shouldContinue;
    }

    @Override
    public void cancel() {
        taskManager.cancelTask(task);
    }

    public void interrupt() {
        if (thread != null) {
            thread.interrupt();
        } else {
            if (!this.futureRef.compareAndSet(NOT_SUBMITED, CANCELLED)) {
                Future<?> future = futureRef.get();
                future.cancel(true);
            }
        }
    }

    @Override
    public void run() {
        shouldContinue = task.pulse();
    }
}
