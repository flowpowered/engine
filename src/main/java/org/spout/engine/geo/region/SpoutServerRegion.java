package org.spout.engine.geo.region;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import org.spout.api.Spout;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.io.bytearrayarray.BAAWrapper;
import org.spout.engine.SpoutEngine;
import org.spout.engine.filesystem.ChunkDataForRegion;
import org.spout.engine.filesystem.ChunkFiles;
import org.spout.engine.geo.SpoutChunk;
import org.spout.engine.scheduler.RenderThread;

public class SpoutServerRegion extends SpoutRegion {
	private final RegionGenerator generator;
	/**
	 * Reference to the persistent ByteArrayArray that stores chunk data
	 */
	private final BAAWrapper chunkStore;

    public SpoutServerRegion(SpoutEngine engine, World world, float x, float y, float z, BAAWrapper chunkStore, RenderThread render) {
        super(engine, world, x, y, z, render);
        this.generator = new RegionGenerator(this, 4);
        this.chunkStore = chunkStore;
    }

    @Override
    public SpoutChunk getChunk(int x, int y, int z, final LoadOption loadopt) {
        x &= CHUNKS.MASK;
        y &= CHUNKS.MASK;
        z &= CHUNKS.MASK;

        SpoutChunk chunk = super.getChunk(x, y, z, loadopt);
        if (chunk != null) {
            return chunk;
        }

        if (!loadopt.loadIfNeeded()) {
            return null;
        }

        if (!engine.getPlatform().isServer()) {
            throw new UnsupportedOperationException("Client cannot load or generate chunks!");
        }

        if (loadopt.isWait()) {
            return loadOrGenChunkImmediately(x, y, z, loadopt);
        }

        final int finalX = x;
        final int finalY = y;
        final int finalZ = z;
        engine.getScheduler().runCoreAsyncTask(new Runnable() {
            @Override
            public void run() {
                loadOrGenChunkImmediately(finalX, finalY, finalZ, loadopt);
            }
        });
        return null;
    }

    // If loadopt.isWait(), this method is run synchronously and so is any further generation
    // If !loadopt.isWait(), this method is run by a runnable, because the loading is taxing; any further generation is also run in its own Runnable
	private SpoutChunk loadOrGenChunkImmediately(int x, int y, int z, final LoadOption loadopt) {
		SpoutChunk newChunk = loadopt.loadIfNeeded() ? loadChunk(x, y, z) : null;

		if (newChunk != null || !loadopt.generateIfNeeded()) {
            return newChunk;
		}

        generator.generateChunk(x, y, z, loadopt.isWait());
        if (!loadopt.isWait()) {
            return null;
        }
        final SpoutChunk generatedChunk = live.get()[getChunkIndex(x, y, z)];
        if (generatedChunk != null) {
            checkChunkLoaded(generatedChunk, loadopt);
            return generatedChunk;
        }
        Spout.getLogger().severe("Chunk failed to generate!  (" + loadopt + ")");
        Spout.getLogger().info("Region " + this + ", chunk " + (getChunkX() + x) + ", " + (getChunkY() + y) + ", " + (getChunkZ() + z));
        Thread.dumpStack();
        return null;
	}

    private SpoutChunk loadChunk(int x, int y, int z) {
        final InputStream stream = this.getChunkInputStream(x, y, z);
        if (stream != null) {
            try {
                try {
                    ChunkDataForRegion dataForRegion = new ChunkDataForRegion();
                    SpoutChunk newChunk = ChunkFiles.loadChunk(this, x, y, z, stream, dataForRegion);
                    if (newChunk == null) {
                        Spout.getLogger().severe("Unable to load chunk at location " + (getChunkX() + x) + ", " + (getChunkY() + y) + ", " + (getChunkZ() + z) + " in region " + this + ", regenerating chunks");
                        return null;
                    }
                    SpoutChunk c = setChunk(newChunk, x, y, z, dataForRegion);
                    checkChunkLoaded(c, LoadOption.LOAD_ONLY);
                    return c;
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                Spout.getLogger().log(Level.WARNING, "IOException when loading chunk!", e);
            }
        }
        return null;
    }

	/**
	 * Gets the DataInputStream corresponding to a given Chunk.<br> <br> The stream is based on a snapshot of the array.
	 *
	 * @param x the chunk
	 * @return the DataInputStream
	 */
	public InputStream getChunkInputStream(int x, int y, int z) {
		return chunkStore.getBlockInputStream(getChunkKey(x, y, z));
	}

	public static int getChunkKey(int chunkX, int chunkY, int chunkZ) {
		chunkX &= CHUNKS.MASK;
		chunkY &= CHUNKS.MASK;
		chunkZ &= CHUNKS.MASK;

		int key = 0;
		key |= chunkX;
		key |= chunkY << CHUNKS.BITS;
		key |= chunkZ << (CHUNKS.BITS << 1);

		return key;
	}
}
