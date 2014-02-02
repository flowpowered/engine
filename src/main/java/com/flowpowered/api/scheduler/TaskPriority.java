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

public class TaskPriority {
    /**
     * Priority for tasks which may not be deferred
     */
    public static final TaskPriority CRITICAL = new TaskPriority(0);
    /**
     * Priority for tasks which can be deferred by up to 50ms when under load
     */
    public static final TaskPriority HIGHEST = new TaskPriority(50);
    /**
     * Priority for tasks which can be deferred by up to 150ms when under load
     */
    public static final TaskPriority HIGH = new TaskPriority(150);
    /**
     * Priority for tasks which can be deferred by up to 500ms when under load
     */
    public static final TaskPriority MEDIUM = new TaskPriority(500);
    /**
     * Priority for tasks which can be deferred by up to 500ms when under load
     */
    public static final TaskPriority NORMAL = MEDIUM;
    /**
     * Priority for tasks which can be deferred by up to 1.5s when under load
     */
    public static final TaskPriority LOW = new TaskPriority(1500);
    /**
     * Priority for tasks which can be deferred by up to 10s when under load
     */
    public static final TaskPriority LOWEST = new TaskPriority(10000);
    private final long maxDeferred;

    /**
     * Creates a TaskPriority instance which sets the maximum time that a task can be deferred.
     *
     * @param maxDelay the maximum delay before
     */
    public TaskPriority(long maxDeferred) {
        this.maxDeferred = maxDeferred;
    }

    /**
     * Gets the maximum time that the task can be deferred.
     */
    public long getMaxDeferred() {
        return maxDeferred;
    }
}
