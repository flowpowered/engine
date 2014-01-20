package com.flowpowered.api.geo;

import java.io.File;
import java.util.List;
import com.flowpowered.api.generator.WorldGenerator;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.math.vector.Vector3f;

public interface ServerWorld extends World {

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
    void unload(boolean save);

	/**
	 * Saves all world data to world data file. <p> Note: World data does not include chunks, regions, or other data. World data pertains to world age, world name, and world data maps. </p>
	 */
	public void save();

	public File getDirectory();

	/**
	 * Queues a list of chunks for generation.  The Vector3 values are in chunk coords.
	 *
	 * @param chunks a list of chunk coordinates
	 */
	public void queueChunksForGeneration(List<Vector3f> chunks);

	/**
	 * Queues a chunk for generation.  The Vector3 value is in chunk coords.
	 *
	 * @param chunk chunk coordinates
	 */
	public void queueChunkForGeneration(Vector3f chunk);
    
}
