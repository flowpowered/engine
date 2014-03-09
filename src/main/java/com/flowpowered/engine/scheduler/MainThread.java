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

import com.flowpowered.api.Server;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.player.Player;
import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.engine.player.FlowPlayer;

public class MainThread extends TickingElement {
    private final FlowScheduler scheduler;

    public MainThread(FlowScheduler scheduler) {
        super("MainThread", 20);
        this.scheduler = scheduler;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public void onTick(long delta) {
        if (scheduler.getEngine().getPlatform().isServer()) {
            for (Player p : ((Server) scheduler.getEngine()).getOnlinePlayers()) {
                Transform transform = p.getTransformProvider().getTransform();
                ((FlowPlayer) p).getNetwork().finalizeRun(transform);
                ((FlowPlayer) p).getNetwork().preSnapshotRun(transform);
            }
        }
        scheduler.getEngine().copySnapshot();
    }
}
