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

import java.util.Arrays;

import org.apache.commons.lang3.SystemUtils;

/**
 * A time class. Calling the {@link #sync()} method at the end of each tick will cause the thread to sleep for the correct time delay between the ticks. {@link #start()} must be called just before the
 * loop to start the timer. {@link #reset()} is used to reset the start time to the current time.
 * <p/>
 * Based on LWJGL's implementation of {@link org.lwjgl.opengl.Sync}.
 */
public class Timer {
    // Time to sleep or yield before next tick
    private long nextTick = -1;
    // Last 10 running averages for sleeps and yields
    private final RunAverages sleepDurations = new RunAverages(10, 1000 * 1000);
    private final RunAverages yieldDurations = new RunAverages(10, (int) (-(getTime() - getTime()) * 1.333f));
    // The target tps
    private final int tps;

    static {
        // Makes windows thread sleeping more accurate
        if (SystemUtils.IS_OS_WINDOWS) {
            final Thread sleepingDaemon = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (Exception ignored) {
                    }
                }
            };
            sleepingDaemon.setName("Timer");
            sleepingDaemon.setDaemon(true);
            sleepingDaemon.start();
        }
    }

    /**
     * Constructs a new timer.
     *
     * @param tps The target tps
     */
    public Timer(int tps) {
        this.tps = tps;
    }

    /**
     * Returns the timer's target TPS.
     * @return The tps
     */
    public int getTps() {
        return tps;
    }

    /**
     * Starts the timer.
     */
    public void start() {
        nextTick = getTime();
    }

    /**
     * Resets the timer.
     */
    public void reset() {
        start();
    }

    /**
     * An accurate sync method that will attempt to run at the tps. It should be called once every tick.
     */
    public void sync() {
        if (nextTick < 0) {
            throw new IllegalStateException("Timer was not started");
        }
        if (tps <= 0) {
            return;
        }
        try {
            // Sleep until the average sleep time is greater than the time remaining until next tick
            for (long time1 = getTime(), time2; nextTick - time1 > sleepDurations.average(); time1 = time2) {
                Thread.sleep(1);
                // Update average sleep time
                sleepDurations.add((time2 = getTime()) - time1);
            }
            // Slowly dampen sleep average if too high to avoid yielding too much
            sleepDurations.dampen();
            // Yield until the average yield time is greater than the time remaining until next tick
            for (long time1 = getTime(), time2; nextTick - time1 > yieldDurations.average(); time1 = time2) {
                Thread.yield();
                // Update average yield time
                yieldDurations.add((time2 = getTime()) - time1);
            }
        } catch (InterruptedException ignored) {
        }
        // Schedule next frame, drop frames if it's too late for next frame
        nextTick = Math.max(nextTick + 1000000000 / tps, getTime());
    }

    // Get the system time in nanoseconds
    private static long getTime() {
        return System.nanoTime();
    }

    // Holds a number of run times for averaging
    private static class RunAverages {
        // Dampen threshold, 10ms
        private static final long DAMPEN_THRESHOLD = 10000000;
        // Dampen factor, don't alter this value
        private static final float DAMPEN_FACTOR = 0.9f;
        private final long[] values;
        private int currentIndex = 0;

        private RunAverages(int slotCount, long initialValue) {
            values = new long[slotCount];
            Arrays.fill(values, initialValue);
        }

        private void add(long value) {
            currentIndex %= values.length;
            values[currentIndex++] = value;
        }

        private long average() {
            long sum = 0;
            for (long slot : values) {
                sum += slot;
            }
            return sum / values.length;
        }

        private void dampen() {
            if (average() > DAMPEN_THRESHOLD) {
                for (int i = 0; i < values.length; i++) {
                    values[i] *= DAMPEN_FACTOR;
                }
            }
        }
    }
}
