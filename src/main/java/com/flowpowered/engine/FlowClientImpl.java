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

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.Platform;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.geo.world.FlowWorldManager;
import com.flowpowered.engine.network.FlowNetworkClient;
import com.flowpowered.engine.player.FlowClientPlayer;
import com.flowpowered.engine.render.DeployNatives;
import com.flowpowered.engine.render.FlowRenderer;

import org.apache.logging.log4j.LogManager;

public class FlowClientImpl extends FlowEngineImpl implements FlowClient {
    private final AtomicReference<FlowClientPlayer> player = new AtomicReference<>();
    private final AtomicReference<FlowWorld> activeWorld = new AtomicReference<>();
    private final FlowWorldManager<FlowWorld> worldManager;
    private final FlowNetworkClient client = new FlowNetworkClient();

    public FlowClientImpl(FlowApplication args) {
        super(args);
        this.worldManager = new FlowWorldManager<>(this);
    }

    @Override
    public void init() {
        try {
            DeployNatives.deploy();
        } catch (Exception ex) {
            LogManager.getLogger(FlowSingleplayer.class.getName()).fatal("", ex);
            return;
        }
        super.init();
        // TEST CODE
        FlowWorld world = new FlowWorld(this, "TestWorld");
        worldManager.addWorld(world);
        getScheduler().addAsyncManager(world);
        activeWorld.set(world);
    }

    @Override
    public void start() {
        client.connect(new InetSocketAddress(25565));
        getScheduler().startClientThreads(this);
        super.start();
    }

    @Override
    public Platform getPlatform() {
        return Platform.CLIENT;
    }

    @Override
    public FlowWorldManager<FlowWorld> getWorldManager() {
        return worldManager;
    }

    @Override
    public FlowClientPlayer getPlayer() {
        return player.get();
    }

    @Override
    public FlowWorld getWorld() {
        return activeWorld.get();
    }

    @Override
    public FlowRenderer getRenderer() {
        return getScheduler().getRenderThread().getRenderer();
    }

}
