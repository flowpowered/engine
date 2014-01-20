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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.flowpowered.commons.Named;
import com.flowpowered.commons.store.block.impl.AtomicPaletteBlockStore;

import com.flowpowered.api.Spout;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.engine.geo.chunk.SpoutChunk;
import com.flowpowered.engine.geo.world.SpoutServerWorld;
import com.flowpowered.engine.util.thread.LoggingThreadPoolExecutor;
import com.flowpowered.math.GenericMath;

public class RegionGenerator implements Named {
	private final static ExecutorService pool = LoggingThreadPoolExecutor.newFixedThreadExecutorWithMarkedName(Runtime.getRuntime().availableProcessors() * 2 + 1, "RegionGenerator - async pool");
	private final SpoutRegion region;
	private final SpoutServerWorld world;
	private final Lock[][][] sectionLocks;
	private final AtomicReference<GenerateState>[][][] generatedChunks;
	private final int shift;
	private final int width;
	private final int mask;
	private final static AtomicInteger generationCounter = new AtomicInteger(1);
	private final int baseChunkX;
	private final int baseChunkY;
	private final int baseChunkZ;

	@SuppressWarnings ("unchecked")
	public RegionGenerator(SpoutRegion region, int width) {
		if (GenericMath.roundUpPow2(width) != width || width > Region.CHUNKS.SIZE || width < 0) {
			throw new IllegalArgumentException("Width must be a power of 2 and can't be more than one region width");
		}

		int sections = Region.CHUNKS.SIZE / width;

		this.width = width;
		this.mask = width - 1;
		this.generatedChunks = new AtomicReference[sections][sections][sections];
		this.sectionLocks = new Lock[sections][sections][sections];

		for (int x = 0; x < sections; x++) {
			for (int z = 0; z < sections; z++) {
                for (int y = 0; y < sections; y++) {
                    this.generatedChunks[x][z][y] = new AtomicReference<>(GenerateState.NONE);
                    this.sectionLocks[x][z][y] = new NamedReentrantLock(x, z);
                }
			}
		}

		this.shift = GenericMath.multiplyToShift(width);
		this.region = region;
		this.world = (SpoutServerWorld) region.getWorld();
		this.baseChunkX = region.getChunkX();
		this.baseChunkY = region.getChunkY();
		this.baseChunkZ = region.getChunkZ();
	}

	/**
	 * 
	 * @param chunkX
     * @param chunkY
     * @param chunkZ
	 * @param wait whether to wait or not
	 */
	public void generateChunk(final int chunkX, final int chunkY, final int chunkZ, boolean wait) {
		if (!wait) {
			pool.submit(new Runnable() {
				@Override
				public void run() {
                    generateChunk0(chunkX, chunkY, chunkZ, false);
				}
			});
		} else {
			generateChunk0(chunkX, chunkY, chunkZ, true);
		}
	}

	private void generateChunk0(final int chunkXWorld, final int chunkYWorld, final int chunkZWorld, boolean wait) {
        // Represent the coords of the section of the region
        // Values are from 0 to width
		final int sectionX = (chunkXWorld & Region.CHUNKS.MASK) >> shift;
		final int sectionY = (chunkYWorld & Region.CHUNKS.MASK) >> shift;
		final int sectionZ = (chunkZWorld & Region.CHUNKS.MASK) >> shift;
        // Represent the local chunk coords of the base of the section
        // Values start at 0 and are spaced by width chunks
		final int chunkXLocal = (chunkXWorld & (~mask)) & Region.CHUNKS.MASK;
		final int chunkYLocal = (chunkYWorld & (~mask)) & Region.CHUNKS.MASK;
		final int chunkZLocal = (chunkZWorld & (~mask)) & Region.CHUNKS.MASK;

		AtomicReference<GenerateState> generated = generatedChunks[sectionX][sectionZ][sectionY];
		if (generated.get().isDone()) {
			return;
		}

		final Lock sectionLock = sectionLocks[sectionX][sectionZ][sectionY];

		if (wait) {
			sectionLock.lock();
		} else {
			if (!sectionLock.tryLock()) {
                // Already being generated by another thread
				return;
			}
		}

		try {
			if (generated.get().isDone()) {
				return;
			}

			int generationIndex = generationCounter.getAndIncrement();

			while (generationIndex == -1) {
				Spout.getLogger().info("Ran out of generation index ids, starting again");
				generationIndex = generationCounter.getAndIncrement();
			}

			if (!generated.compareAndSet(GenerateState.NONE, GenerateState.IN_PROGRESS)) {
                throw new IllegalStateException("Unable to set generate state for column " + sectionX + ", " + sectionY +", " + sectionZ + " in region " + region.getBase().toBlockString() + " to in progress, state is " + generated.get() + " wait is " + wait);
			}

			int chunkInWorldX = baseChunkX + chunkXLocal;
			int chunkInWorldY = baseChunkY + chunkYLocal;
			int chunkInWorldZ = baseChunkZ + chunkZLocal;

			final CuboidBlockMaterialBuffer buffer = new CuboidBlockMaterialBuffer(chunkInWorldX << Chunk.BLOCKS.BITS, chunkInWorldY << Chunk.BLOCKS.BITS, chunkInWorldZ << Chunk.BLOCKS.BITS, Chunk.BLOCKS.SIZE << shift, Chunk.BLOCKS.SIZE << shift, Chunk.BLOCKS.SIZE << shift);
			world.getGenerator().generate(buffer, world);

			SpoutChunk[][][] chunks = new SpoutChunk[width][width][width];
			for (int xx = 0; xx < width; xx++) {
				chunkInWorldX = baseChunkX + chunkXLocal + xx;
				for (int zz = 0; zz < width; zz++) {
					chunkInWorldZ = baseChunkZ + chunkZLocal + zz;
					for (int yy = 0 ; yy < width; yy++) {
						chunkInWorldY = baseChunkY + chunkYLocal + yy;
						final CuboidBlockMaterialBuffer chunkBuffer = new CuboidBlockMaterialBuffer(chunkInWorldX << Chunk.BLOCKS.BITS, chunkInWorldY << Chunk.BLOCKS.BITS, chunkInWorldZ << Chunk.BLOCKS.BITS, Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE);
						chunkBuffer.write(buffer);
						SpoutChunk newChunk = new SpoutChunk(region, world, chunkInWorldX, chunkInWorldY, chunkInWorldZ, generationIndex, new AtomicPaletteBlockStore(Chunk.BLOCKS.BITS, true, true, 10, chunkBuffer.getRawId(), chunkBuffer.getRawData()));
						chunks[xx][yy][zz] = newChunk;
					}

				}
			}

			if (!generated.compareAndSet(GenerateState.IN_PROGRESS, GenerateState.COPYING)) {
                throw new IllegalStateException("Unable to set generate state for column " + sectionX + ", " + sectionY +", " + sectionZ + " in region " + region.getBase().toBlockString() + " to copying, state is " + generated.get() + " wait is " + wait);
			}
            region.setGeneratedChunks(chunks, chunkXLocal, chunkYLocal, chunkZLocal);

            // We need to set the generated state before we unlock the readLock so waiting generators get the state immediately
            if (!generated.compareAndSet(GenerateState.COPYING, GenerateState.COPIED)) {
                throw new IllegalStateException("Unable to set generate state for column " + sectionX + ", " + sectionY +", " + sectionZ + " in region " + region.getBase().toBlockString() + " copied twice after generation, generation state is " + generated + " wait is " + wait);
            }
		} finally {
			sectionLock.unlock();
		}
	}

	public static void shutdownExecutorService() {
		pool.shutdown();
	}

	public static void awaitExecutorServiceTermination() {
		boolean interrupted = false;
		try {
			boolean done = false;
			while (!done) {
				try {
					if (pool.awaitTermination(10, TimeUnit.SECONDS)) {
						done = true;
						break;
					}
					Spout.getLogger().info("Waited 10 seconds for region generator pool to shutdown");
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static enum GenerateState {
		NONE,
		IN_PROGRESS,
		COPYING,
		COPIED;

		public boolean isDone() {
			return this == COPIED;
		}

		public boolean isInProgress() {
			return this == IN_PROGRESS || this == COPYING;
		}

	}

	private class NamedReentrantLock extends ReentrantLock implements Named {
		private static final long serialVersionUID = 1L;
		private final int x;
		private final int z;

		public NamedReentrantLock(int x, int z) {
			this.x = x;
			this.z = z;
		}

		@Override
		public String getName() {
			return "NamedReentrantLock{(" + x + ", " + z + "), " + region + "}";
		}
	}

	@Override
	public String getName() {
		return "RegionGenerator{" + region + "}";
	}
}
