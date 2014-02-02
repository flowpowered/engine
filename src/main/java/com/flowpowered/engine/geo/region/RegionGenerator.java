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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Logger;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.commons.Named;
import com.flowpowered.commons.store.block.impl.AtomicPaletteBlockStore;
import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.geo.world.FlowServerWorld;
import com.flowpowered.engine.util.thread.LoggingThreadPoolExecutor;
import com.flowpowered.math.GenericMath;

public class RegionGenerator implements Named {
    private static AtomicReference<ExecutorService> pool = new AtomicReference<>();
    private final FlowRegion region;
    private final FlowServerWorld world;
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
    public RegionGenerator(FlowRegion region, int width) {
        initExecutorService(region.getWorld().getEngine().getLogger());
        if (GenericMath.roundUpPow2(width) != width || width > Region.CHUNKS.SIZE || width < 0) {
            throw new IllegalArgumentException("Width must be a power of 2 and can't be more than one region width");
        }

        int sections = Region.CHUNKS.SIZE / width;

        this.width = width;
        mask = width - 1;
        generatedChunks = new AtomicReference[sections][sections][sections];
        sectionLocks = new Lock[sections][sections][sections];

        for (int x = 0; x < sections; x++) {
            for (int z = 0; z < sections; z++) {
                for (int y = 0; y < sections; y++) {
                    generatedChunks[x][z][y] = new AtomicReference<>(GenerateState.NONE);
                    sectionLocks[x][z][y] = new NamedReentrantLock(x, z);
                }
            }
        }

        shift = GenericMath.multiplyToShift(width);
        this.region = region;
        world = (FlowServerWorld) region.getWorld();
        baseChunkX = region.getChunkX();
        baseChunkY = region.getChunkY();
        baseChunkZ = region.getChunkZ();
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
            pool.get().submit(new Runnable() {
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
                world.getEngine().getLogger().info("Ran out of generation index ids, starting again");
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

            FlowChunk[][][] chunks = new FlowChunk[width][width][width];
            for (int xx = 0; xx < width; xx++) {
                chunkInWorldX = baseChunkX + chunkXLocal + xx;
                for (int zz = 0; zz < width; zz++) {
                    chunkInWorldZ = baseChunkZ + chunkZLocal + zz;
                    for (int yy = 0 ; yy < width; yy++) {
                        chunkInWorldY = baseChunkY + chunkYLocal + yy;
                        final CuboidBlockMaterialBuffer chunkBuffer = new CuboidBlockMaterialBuffer(chunkInWorldX << Chunk.BLOCKS.BITS, chunkInWorldY << Chunk.BLOCKS.BITS, chunkInWorldZ << Chunk.BLOCKS.BITS, Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE);
                        chunkBuffer.write(buffer);
                        FlowChunk newChunk = new FlowChunk(region, world, chunkInWorldX, chunkInWorldY, chunkInWorldZ, generationIndex, new AtomicPaletteBlockStore(Chunk.BLOCKS.BITS, true, true, 10, chunkBuffer.getRawId(), chunkBuffer.getRawData()));
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
    
    private static void initExecutorService(Logger logger) {
        if (pool.get() == null) {
            pool.compareAndSet(null, LoggingThreadPoolExecutor.newFixedThreadExecutorWithMarkedName(Runtime.getRuntime().availableProcessors() * 2 + 1, "RegionGenerator - async pool", logger));
        }
    }

    public static void shutdownExecutorService() {
        ExecutorService p = pool.get();
        if (p != null) {
            p.shutdown();
        }
    }

    public static void awaitExecutorServiceTermination(Logger logger) {
        boolean interrupted = false;
        ExecutorService p = pool.get();
        if (p == null) {
            return;
        }
        try {
            boolean done = false;
            while (!done) {
                try {
                    if (p.awaitTermination(10, TimeUnit.SECONDS)) {
                        done = true;
                        break;
                    }
                    logger.info("Waited 10 seconds for region generator pool to shutdown");
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
