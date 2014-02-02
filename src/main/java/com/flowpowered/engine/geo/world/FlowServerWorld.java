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
package com.flowpowered.engine.geo.world;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.generator.WorldGenerator;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.ServerWorld;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.io.bytearrayarray.BAAWrapper;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.filesystem.WorldFiles;
import com.flowpowered.engine.geo.region.RegionFileManager;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.engine.filesystem.FlowFileSystem;

public class FlowServerWorld extends FlowWorld implements ServerWorld {
    private final WorldGenerator generator;
    private final long seed;
    /**
     * The spawn position.
     */
    private final AtomicReference<Transform> spawnLocation = new AtomicReference<>();
    /**
     * RegionFile manager for the world
     */
    private final RegionFileManager regionFileManager;

    public FlowServerWorld(FlowEngine engine, String name, UUID uid, long age, WorldGenerator generator, long seed) {
        super(engine, name, uid, age);
        this.spawnLocation.set(new Transform(new Point(this, 0, 0, 0), Quaternionf.IDENTITY, Vector3f.ONE));
        this.generator = generator;
        this.seed = seed;
        this.regionFileManager = new RegionFileManager(new File(FlowFileSystem.WORLDS_DIRECTORY, name), engine.getLogger());
    }

    public FlowServerWorld(FlowEngine engine, String name, WorldGenerator generator) {
        super(engine, name);
        this.spawnLocation.set(new Transform(new Point(this, 0, 0, 0), Quaternionf.IDENTITY, Vector3f.ONE));
        this.generator = generator;
        this.seed = new Random().nextLong();
        this.regionFileManager = new RegionFileManager(new File(FlowFileSystem.WORLDS_DIRECTORY, name), engine.getLogger());
    }

    @Override
    public WorldGenerator getGenerator() {
        return generator;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    @Override
    public Transform getSpawnPoint() {
        return spawnLocation.get();
    }

    @Override
    public void setSpawnPoint(Transform transform) {
        spawnLocation.set(transform);
    }

    @Override
    public void unload(boolean save) {
    }

    @Override
    public void save() {
        WorldFiles.saveWorld(this);
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public File getDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void queueChunksForGeneration(List<Vector3f> chunks) {
        for (Vector3f chunk : chunks) {
            queueChunkForGeneration(chunk);
        }
    }

    @Override
    public void queueChunkForGeneration(Vector3f chunk) {
        getChunk(chunk.getFloorX(), chunk.getFloorY(), chunk.getFloorZ(), LoadOption.LOAD_GEN_NOWAIT);
    }

    public BAAWrapper getRegionFile(int rx, int ry, int rz) {
        if (regionFileManager == null) {
            throw new IllegalStateException("Client does not have file manager");
        }
        return regionFileManager.getBAAWrapper(rx, ry, rz);
    }
}
