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
package com.flowpowered.api.scheduler;

/**
 * Represents a worker thread for the scheduler. This gives information about the Thread object for the task, owner of the task and the taskId.
 *
 * Workers are used to execute async tasks.
 */
public interface Worker {
    /**
     * Returns the taskId for the task being executed by this worker
     *
     * @return Task id number
     */
    public int getTaskId();

    /**
     * Returns the Object that owns this task
     *
     * @return The Object that owns the task
     */
    public Object getOwner();

    /**
     * Attempts to cancel the task.  This will trigger an interrupt for async tasks that are in progress.
     */
    public void cancel();

    /**
     * Gets the task associated with this worker
     *
     * @return the task
     */
    public Task getTask();
}
