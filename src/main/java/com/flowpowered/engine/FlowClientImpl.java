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

import org.apache.logging.log4j.LogManager;

import org.spout.renderer.lwjgl.LWJGLUtil;

import com.flowpowered.api.Platform;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.geo.world.FlowWorldManager;
import com.flowpowered.engine.network.FlowNetworkClient;
import com.flowpowered.engine.network.FlowSession;
import com.flowpowered.engine.render.FlowRenderer;

public class FlowClientImpl extends FlowEngineImpl implements FlowClient {
    private final FlowWorldManager<FlowWorld> worldManager;
    private final FlowNetworkClient client = new FlowNetworkClient(this);

    private volatile Transform transform = Transform.INVALID;

    public FlowClientImpl(FlowApplication args) {
        super(args);
        this.worldManager = new FlowWorldManager<>(this);
    }

    @Override
    public void init() {
        try {
            LWJGLUtil.deployNatives(null);
        } catch (Exception ex) {
            LogManager.getLogger(FlowSingleplayer.class.getName()).fatal("", ex);
            return;
        }
        super.init();
        // TEST CODE
        FlowWorld world = new FlowWorld(this, "TestWorld");
        worldManager.addWorld(world);
        getScheduler().addAsyncManager(world);
    }

    @Override
    public void start() {
        client.connect(new InetSocketAddress(25565));
        getScheduler().startClientThreads(this);
        super.start();
    }

    @Override
    public boolean stop() {
        client.shutdown();
        return super.stop();
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
    public FlowRenderer getRenderer() {
        return getScheduler().getRenderThread().getRenderer();
    }

    @Override
    public Transform getTransform() {
        return transform;
    }

    @Override
    public void setTransform(Transform transform) {
        this.transform = transform;
    }

    @Override
    public FlowSession getSession() {
        return client.getSession();
    }

}
