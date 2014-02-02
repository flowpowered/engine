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
package com.flowpowered.api.geo;

import java.io.File;
import java.util.List;
import com.flowpowered.api.generator.WorldGenerator;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.util.UnloadSavable;
import com.flowpowered.math.vector.Vector3f;

public interface ServerWorld extends World, UnloadSavable {

    /**
     * Gets the {@link WorldGenerator} responsible for generating new chunks for this world
     *
     * @return generator
     */
    WorldGenerator getGenerator();

    /**
     * Gets the world's seed. This value is immutable and set at world creation
     *
     * @return the seed
     */
    long getSeed();

    // Techinically server-only
    /**
     * Gets the world's spawn point
     *
     * @return the spawn point
     */
    Transform getSpawnPoint();

    /**
     * Sets the world's spawn point
     *
     * @param transform the Transform of the spawn point
     */
    void setSpawnPoint(Transform transform);

    /**
     * Unloads the world from the server. Undefined behavior will occur if any players are currently alive on the world while it is being unloaded.
     */
    @Override
    void unload(boolean save);

    /**
     * Saves all world data to world data file. <p> Note: World data does not include chunks, regions, or other data. World data pertains to world age, world name, and world data maps. </p>
     */
    @Override
    void save();

    File getDirectory();

    /**
     * Queues a list of chunks for generation.  The Vector3 values are in chunk coords.
     *
     * @param chunks a list of chunk coordinates
     */
    void queueChunksForGeneration(List<Vector3f> chunks);

    /**
     * Queues a chunk for generation.  The Vector3 value is in chunk coords.
     *
     * @param chunk chunk coordinates
     */
    void queueChunkForGeneration(Vector3f chunk);
    
}
