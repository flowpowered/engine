/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package com.flowpowered.engine.geo.region;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.commons.concurrent.TripleIntObjectMap;
import com.flowpowered.commons.concurrent.TripleIntObjectReferenceArrayMap;

import com.flowpowered.api.Spout;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.ServerWorld;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.scheduler.TickStage;
import com.flowpowered.api.util.thread.annotation.DelayedWrite;
import com.flowpowered.api.util.thread.annotation.LiveRead;
import com.flowpowered.engine.SpoutEngine;
import com.flowpowered.engine.geo.world.SpoutServerWorld;
import com.flowpowered.engine.geo.world.SpoutWorld;

public class RegionSource implements Iterable<Region> {
	private final static int REGION_MAP_BITS = 5;
	private final static AtomicInteger regionsLoaded = new AtomicInteger(0);
	/**
	 * A map of loaded regions, mapped to their x and z values.
	 */
	private final TripleIntObjectMap<SpoutRegion> loadedRegions;
	/**
	 * World associated with this region source
	 */
	private final SpoutWorld world;
    private final SpoutEngine engine;

	public RegionSource(SpoutEngine engine, SpoutWorld world) {
        this.engine = engine;
		this.world = world;
		loadedRegions = new TripleIntObjectReferenceArrayMap<>(REGION_MAP_BITS);
	}

	@DelayedWrite
	public void removeRegion(final SpoutRegion r) {
		TickStage.checkStage(TickStage.SNAPSHOT);

		if (!r.getWorld().equals(world)) {
			throw new IllegalArgumentException("Provided region's world is not the same world as this RegionSource's world!");
		}

        /*
        if (!r.isEmpty()) {
            Spout.getLogger().info("Region was not empty when attempting to remove, active chunks returns " + r.getNumLoadedChunks());
            return;
        }
        if (!r.attemptClose()) {
            Spout.getLogger().info("Unable to close region file, streams must be open");
            return;
        }
        */
        int x = r.getX();
        int y = r.getY();
        int z = r.getZ();
        boolean success = loadedRegions.remove(x, y, z, r);
        if (!success) {
            Spout.getLogger().info("Tried to remove region " + r + " but region removal failed");
            return;
        }

        world.getEngine().getScheduler().removeAsyncManager(r);

        if (regionsLoaded.decrementAndGet() < 0) {
            Spout.getLogger().info("Regions loaded dropped below zero");
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
	public SpoutRegion getRegion(int x, int y, int z, LoadOption loadopt) {
		if (loadopt != LoadOption.NO_LOAD) {
			TickStage.checkStage(TickStage.noneOf(TickStage.SNAPSHOT));
		}

		SpoutRegion region = loadedRegions.get(x, y, z);

		if (region != null) {
			return region;
		}

		if (!loadopt.loadIfNeeded()) {
			return null;
		}

        SpoutServerWorld serverWorld = (SpoutServerWorld) world;
		region = new SpoutRegion(engine, world, x, y, z, serverWorld.getRegionFile(x, y, z), engine.getPlatform().isClient() ? engine.getScheduler().getRenderThread() : null);
		SpoutRegion current = loadedRegions.putIfAbsent(x, y, z, region);

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
		if (!Spout.getPlatform(). isServer()) {
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
