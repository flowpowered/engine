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

import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.material.MaterialRegistry;
import com.flowpowered.engine.geo.world.FlowServerWorld;
import com.flowpowered.engine.network.FlowNetworkClient;
import com.flowpowered.engine.network.FlowSession;

public class FlowClientImpl extends AbstractFlowClientImpl {
    private final FlowNetworkClient client;

    private volatile Transform transform = Transform.INVALID;

    static {
        try {
            LWJGLUtil.deployNatives(null);
        } catch (Exception ex) {
            LogManager.getLogger(FlowSingleplayer.class.getName()).fatal("", ex);
        }
    }

    public FlowClientImpl(FlowEngineImpl engine) {
        super(engine);
        this.client = new FlowNetworkClient(engine);
    }

    @Override
    public void onAdd() {
        if (!MaterialRegistry.isSetup()) {
            MaterialRegistry.setupClient();
        }
        // TEST CODE
        FlowServerWorld world = new FlowServerWorld(engine, "fallback", null);
        engine.getWorldManager().addWorld(world);
        world.getThread().start();
        client.connect(new InetSocketAddress(engine.getArgs().server, engine.getArgs().port));
        super.onAdd();
    }

    @Override
    public void stop(String reason) {
        client.shutdown();
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
