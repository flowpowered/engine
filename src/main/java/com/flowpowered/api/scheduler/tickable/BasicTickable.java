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
package com.flowpowered.api.scheduler.tickable;

public abstract class BasicTickable implements Tickable {
    @Override
    public final void tick(float dt) {
        if (canTick()) {
            onTick(dt);
        }
    }

    /**
     * Called each simulation tick.<br/> Override this to perform logic upon ticking.<br/> 1       tick  = 1/20 second<br/> 20      ticks = 1 second<br/> 1200    ticks = 1 minute<br/> 72000   ticks = 1
     * hour<br/> 1728000 ticks = 1 day
     *
     * @param dt time since the last tick in seconds
     */
    public abstract void onTick(float dt);

    /**
     * Whether or not this tickable can perform a tick
     *
     * @return true if it can tick, false if not
     */
    public abstract boolean canTick();
}
