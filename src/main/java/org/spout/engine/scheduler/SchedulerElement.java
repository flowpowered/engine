/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spoutcraft <http://spoutcraft.org/>
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
package org.spout.engine.scheduler;

/**
 * Represents an element that ticks at a specific TPS internally
 */
public abstract class SchedulerElement {
    private final String name;
    private final int tps;
    private TPSLimitedThread thread;

    public SchedulerElement(String name, int tps) {
        this.name = name;
        this.tps = tps;
    }

    public final void start() {
        thread = new TPSLimitedThread(name, this, tps);
        thread.start();
    }

    public final void stop() {
        if (thread != null) {
            thread.terminate();
            thread = null;
        }
    }

    public final boolean isRunning() {
        return thread != null && thread.isRunning();
    }

    public abstract void onStart();

    /**
     * @param dt delta in nanoseconds
     */
    public abstract void onTick(long dt);

    public abstract void onStop();
}
