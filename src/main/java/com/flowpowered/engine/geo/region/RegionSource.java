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
package com.flowpowered.engine.geo.region;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.commons.concurrent.TripleIntObjectMap;
import com.flowpowered.commons.concurrent.TripleIntObjectReferenceArrayMap;

import com.flowpowered.api.Flow;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.ServerWorld;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.geo.world.FlowServerWorld;
import com.flowpowered.engine.geo.world.FlowWorld;

public class RegionSource implements Iterable<Region> {
    private final static int REGION_MAP_BITS = 5;
    private final static AtomicInteger regionsLoaded = new AtomicInteger(0);
    /**
     * A map of loaded regions, mapped to their x and z values.
     */
    private final TripleIntObjectMap<FlowRegion> loadedRegions;
    /**
     * World associated with this region source
     */
    private final FlowWorld world;
    private final FlowEngine engine;

    public RegionSource(FlowEngine engine, FlowWorld world) {
        this.engine = engine;
        this.world = world;
        loadedRegions = new TripleIntObjectReferenceArrayMap<>(REGION_MAP_BITS);
    }

    @DelayedWrite
    public void removeRegion(final FlowRegion r) {
        TickStage.checkStage(TickStage.SNAPSHOT);

        if (!r.getWorld().equals(world)) {
            throw new IllegalArgumentException("Provided region's world is not the same world as this RegionSource's world!");
        }

        /*
        if (!r.isEmpty()) {
            Flow.getLogger().info("Region was not empty when attempting to remove, active chunks returns " + r.getNumLoadedChunks());
            return;
        }
        if (!r.attemptClose()) {
            Flow.getLogger().info("Unable to close region file, streams must be open");
            return;
        }
        */
        int x = r.getRegionX();
        int y = r.getRegionY();
        int z = r.getRegionZ();
        boolean success = loadedRegions.remove(x, y, z, r);
        if (!success) {
            engine.getLogger().info("Tried to remove region " + r + " but region removal failed");
            return;
        }

        world.getEngine().getScheduler().removeAsyncManager(r);

        if (regionsLoaded.decrementAndGet() < 0) {
            engine.getLogger().info("Regions loaded dropped below zero");
        }
    }

    /**
     * Gets the region associated with the region x, y, z coordinates <br/> <p> Will load or generate a region if requested.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param loadopt if {@code loadopt.loadIfNeeded} is false, this may return null
     * @return region
     */
    @LiveRead
    public FlowRegion getRegion(int x, int y, int z, LoadOption loadopt) {
        if (loadopt != LoadOption.NO_LOAD) {
            TickStage.checkStage(TickStage.noneOf(TickStage.SNAPSHOT));
        }

        FlowRegion region = loadedRegions.get(x, y, z);

        if (region != null) {
            return region;
        }

        if (!loadopt.loadIfNeeded()) {
            return null;
        }

        FlowServerWorld serverWorld = (FlowServerWorld) world;
        region = new FlowRegion(engine, world, x, y, z, serverWorld.getRegionFile(x, y, z), engine.getPlatform().isClient() ? engine.getScheduler().getRenderThread() : null);
        FlowRegion current = loadedRegions.putIfAbsent(x, y, z, region);

        if (current != null) {
            return current;
        }

        world.getEngine().getScheduler().addAsyncManager(region);
        return region;
    }
    /**
     * Test if region file exists
     *
     * @param world world
     * @param x region x coordinate
     * @param y region y coordinate
     * @param z region z coordinate
     * @return true if exists, false if doesn't exist
     */
    public static boolean regionFileExists(World world, int x, int y, int z) {
        if (!Flow.getPlatform(). isServer()) {
            return false;
        }
        File worldDirectory = ((ServerWorld) world).getDirectory();
        File regionDirectory = new File(worldDirectory, "region");
        File regionFile = new File(regionDirectory, "reg" + x + "_" + y + "_" + z + ".spr");
        return regionFile.exists();
    }

    /**
     * True if there is a region loaded at the region x, y, z coordinates
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return true if there is a region loaded
     */
    @LiveRead
    public boolean hasRegion(int x, int y, int z) {
        return loadedRegions.get(x, y, z) != null;
    }

    /**
     * Gets an unmodifiable collection of all loaded regions.
     *
     * @return collection of all regions
     */
    @SuppressWarnings("unchecked")
    public Collection<Region> getRegions() {
        return (Collection) Collections.unmodifiableCollection(loadedRegions.valueCollection());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Region> iterator() {
        return ((Collection) getRegions()).iterator();
    }
}
