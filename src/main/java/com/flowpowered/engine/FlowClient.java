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
package com.flowpowered.engine;

import java.util.concurrent.atomic.AtomicReference;
import com.flowpowered.api.Client;
import com.flowpowered.api.Platform;
import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldManager;
import com.flowpowered.api.render.Renderer;
import com.flowpowered.engine.entity.FlowPlayer;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.geo.world.FlowWorldManager;

public class FlowClient extends FlowEngine implements Client {
    private final AtomicReference<FlowPlayer> player = new AtomicReference<>();
    private final AtomicReference<FlowWorld> activeWorld = new AtomicReference<>();
    private final FlowWorldManager<FlowWorld> worldManager;

    public FlowClient(FlowApplication args) {
        super(args);
        this.worldManager = new FlowWorldManager<>(this);
    }

    @Override
    public Platform getPlatform() {
        return Platform.CLIENT;
    }

    @Override
    public WorldManager getWorldManager() {
        return worldManager;
    }

    @Override
    public Player getPlayer() {
        return player.get();
    }

    @Override
    public World getWorld() {
        return activeWorld.get();
    }

    @Override
    public Renderer getRenderer() {
        return getScheduler().getRenderThread().getRenderer();
    }

}
