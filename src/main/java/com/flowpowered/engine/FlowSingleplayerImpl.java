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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.flowpowered.api.Platform;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.generator.FlatWorldGenerator;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.engine.entity.FlowPlayer;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.render.DeployNatives;
import com.flowpowered.engine.render.FlowRenderer;

import com.flowpowered.math.vector.Vector3f;

public class FlowSingleplayerImpl extends FlowServerImpl implements FlowSingleplayer {
    private final AtomicReference<FlowPlayer> player = new AtomicReference<>();
    private final AtomicReference<FlowWorld> activeWorld = new AtomicReference<>();

    // TEST CODE
    private Entity testEntity;
    private Entity testEntity2;

    public FlowSingleplayerImpl(FlowApplication args) {
        super(args);
    }

    @Override
    public void init() {
        try {
            DeployNatives.deploy();
        } catch (Exception ex) {
            Logger.getLogger(FlowSingleplayerImpl.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        super.init();
        FlowWorld loadedWorld = getWorldManager().loadWorld("fallback", new FlatWorldGenerator(BlockMaterial.SOLID_BLUE));
        activeWorld.set(loadedWorld);
        FlowPlayer player = new FlowPlayer("Flowy");
        this.player.set(player);
        players.put(player.getName(), player);
        Entity entity = loadedWorld.spawnEntity(new Vector3f(0, 1, 0), LoadOption.LOAD_GEN);
        Entity entity2 = loadedWorld.spawnEntity(new Vector3f(0, 1, 0), LoadOption.LOAD_GEN);
        this.testEntity = entity;
        this.testEntity2 = entity2;
        player.setTransformProvider(entity.getPhysics());
    }

    public Entity getTestEntity() {
        return testEntity;
    }

    public Entity getTestEntity2() {
        return testEntity2;
    }

    @Override
    public void start() {
        getScheduler().startClientThreads(this);
        super.start();
    }

    @Override
    public Platform getPlatform() {
        return Platform.SINGLEPLAYER;
    }

    @Override
    public FlowPlayer getPlayer() {
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
