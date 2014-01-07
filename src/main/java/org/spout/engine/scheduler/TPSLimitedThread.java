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

import org.spout.api.scheduler.Timer;

/**
 * Represents a thread that runs at a specific TPS until terminated.
 */
public class TPSLimitedThread extends Thread {
    private static final long NANOS_IN_SECOND = 1000000000;
    private final SchedulerElement element;
    private final Timer timer;
    private volatile boolean running = true;

    public TPSLimitedThread(String name, SchedulerElement element, int tps) {
        super(name);
        this.element = element;
        timer = new Timer(tps);
    }

    @Override
    public void run() {
        element.onStart();
        timer.start();
        long currentTime;
        long lastTime = getTime() - (long) (1f / timer.getTps() * NANOS_IN_SECOND);
        while (running) {
            // We put this before everything so that the Timer accurately schedules the first tick
            timer.sync();
            element.onTick((currentTime = getTime()) - lastTime);
            lastTime = currentTime;
        }
        element.onStop();
    }

    public void terminate() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    private static long getTime() {
        return System.nanoTime();
    }
}
