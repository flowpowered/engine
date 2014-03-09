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
package com.flowpowered.engine.util.thread.coretasks;

import com.flowpowered.engine.scheduler.WorldTickStage;
import com.flowpowered.engine.util.thread.AsyncManager;
import com.flowpowered.engine.util.thread.DynamicUpdateManager;

public class LocalDynamicUpdatesTask extends LocalManagerRunnableFactory {
    private long threshold = 0;

    @Override
    public ManagerRunnable getTask(final AsyncManager manager, final int sequence) {
        return new ManagerRunnable(manager) {
            @Override
            public void runTask() {
                ((DynamicUpdateManager) manager).runDynamicUpdates(threshold, sequence);
            }
        };
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    @Override
    public WorldTickStage getTickStage() {
        return WorldTickStage.DYNAMIC_BLOCKS;
    }
}