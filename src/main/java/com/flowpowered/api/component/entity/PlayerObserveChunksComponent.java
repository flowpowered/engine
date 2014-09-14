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
package com.flowpowered.api.component.entity;

import java.util.Set;

import com.flowpowered.api.component.Component;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.event.EntityStartObservingChunksEvent;
import com.flowpowered.api.event.EntityStopObservingChunksEvent;
import com.flowpowered.api.geo.reference.ChunkReference;
import com.flowpowered.api.player.Player;
import com.flowpowered.events.EventHandler;

public class PlayerObserveChunksComponent extends Component {
    private Player controller;

    @Override
    public void onTick(float dt) {
    }

    @Override
    public boolean canTick() {
        return false;
    }

    public void setController(Player player) {
        Set<ChunkReference> chunks = ((Entity) getOwner()).getObserver().getObservingChunks();
        if (this.controller != null) {
            controller.getNetwork().removeChunks(chunks);
        }
        this.controller = player;
        if (this.controller != null) {
            controller.getNetwork().addChunks(chunks);
        }
    }

    @EventHandler
    public void onObserve(EntityStartObservingChunksEvent e) {
        if (e.getObserver().equals(getOwner()) && controller != null) {
            controller.getNetwork().addChunks(e.getObserved());
        }
    }

    @EventHandler
    public void onStopObserve(EntityStopObservingChunksEvent e) {
        if (e.getObserver().equals(getOwner()) && controller != null) {
            controller.getNetwork().removeChunks(e.getObserved());
        }
    }
}
