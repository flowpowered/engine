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
package com.flowpowered.api.io.regionfile;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class SRFBlockLock implements Lock {
    private final AtomicInteger lockCounter;
    private final Lock lock;

    public SRFBlockLock(Lock lock, AtomicInteger lockCounter) {
        this.lock = lock;
        this.lockCounter = lockCounter;
    }

    @Override
    public void lock() {
        incrementLockCounter();
        lock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException("");
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("");
    }

    @Override
    public void unlock() {
        lock.unlock();
        decrementLockCounter();
    }

    /**
     * Increments the lock counter.<br>
     *
     * @return the number of blocks locked or FILE_CLOSED
     */
    private int incrementLockCounter() {
        while (true) {
            int oldValue = this.lockCounter.get();

            if (oldValue == SimpleRegionFile.FILE_CLOSED) {
                return SimpleRegionFile.FILE_CLOSED;
            }

            int newValue = oldValue + 1;
            if (this.lockCounter.compareAndSet(oldValue, newValue)) {
                return newValue;
            }
        }
    }

    /**
     * Increments the lock counter.<br>
     *
     * @return the number of blocks locked or FILE_CLOSED
     */
    private int decrementLockCounter() {
        while (true) {
            int oldValue = this.lockCounter.get();

            int newValue = oldValue - 1;

            if (oldValue == SimpleRegionFile.FILE_CLOSED) {
                newValue = SimpleRegionFile.FILE_CLOSED;
            } else if (oldValue <= 0) {
                throw new RuntimeException("Attempt made to decrement lock counter below zero");
            }

            if (this.lockCounter.compareAndSet(oldValue, newValue)) {
                return newValue;
            }
        }
    }
}
