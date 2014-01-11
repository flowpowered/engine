package org.spout.engine.geo;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.spout.api.generator.WorldGenerator;
import org.spout.api.geo.ServerWorld;
import org.spout.api.geo.discrete.Transform;
import org.spout.engine.SpoutEngine;
import org.spout.engine.filesystem.WorldFiles;
import org.spout.math.vector.Vector3f;

public class SpoutServerWorld extends SpoutWorld implements ServerWorld {
    private final WorldGenerator generator;
    private final long seed;
	/**
	 * The spawn position.
	 */
	private final Transform spawnLocation = new Transform();

    public SpoutServerWorld(SpoutEngine engine, String name, UUID uid, long age, WorldGenerator generator, long seed) {
        super(engine, name, uid, age);
        this.generator = generator;
        this.seed = seed;
    }

    public SpoutServerWorld(SpoutEngine engine, String name, WorldGenerator generator) {
        super(engine, name);
        this.generator = generator;
        this.seed = new Random().nextLong();
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
        return spawnLocation;
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
    public File getDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void queueChunksForGeneration(List<Vector3f> chunks) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void queueChunkForGeneration(Vector3f chunk) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
