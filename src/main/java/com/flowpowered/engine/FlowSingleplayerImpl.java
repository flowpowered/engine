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

import org.apache.logging.log4j.LogManager;

import com.flowpowered.api.Platform;
import com.flowpowered.api.component.entity.PlayerControlledMovementComponent;
import com.flowpowered.api.generator.FlatWorldGenerator;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.engine.player.FlowPlayer;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.network.FlowSingeplayerSession;
import com.flowpowered.engine.player.FlowSingleplayerPlayer;
import com.flowpowered.engine.render.DeployNatives;
import com.flowpowered.engine.render.FlowRenderer;

public class FlowSingleplayerImpl extends FlowServerImpl implements FlowSingleplayer {
    private final AtomicReference<FlowSingleplayerPlayer> player = new AtomicReference<>();
    private final AtomicReference<FlowWorld> activeWorld = new AtomicReference<>();

    public FlowSingleplayerImpl(FlowApplication args) {
        super(args);
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

        FlowWorld loadedWorld = getWorldManager().loadWorld("fallback", new FlatWorldGenerator(BlockMaterial.SOLID_BLUE));
        activeWorld.set(loadedWorld);

        FlowPlayer serverPlayer = new FlowPlayer(new FlowSingeplayerSession(true), "Flowy");
        addPlayer(serverPlayer);

        FlowSingleplayerPlayer clientPlayer = new FlowSingleplayerPlayer(new FlowSingeplayerSession(false), serverPlayer);
        this.player.set(clientPlayer);
    }

    @Override
    public void start() {
        getScheduler().startClientThreads(this);
        super.start();
    }

    @Override
    public boolean stop() {
        return super.stop();
        
    }

    @Override
    public Platform getPlatform() {
        return Platform.SINGLEPLAYER;
    }

    @Override
    public FlowSingleplayerPlayer getPlayer() {
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
