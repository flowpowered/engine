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
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

import com.flowpowered.api.Flow;
import com.flowpowered.api.geo.cuboid.Region;
import com.flowpowered.api.io.bytearrayarray.BAAWrapper;
import com.flowpowered.engine.geo.snapshot.ChunkSnapshot;

public class RegionFileManager {
    /**
     * The segment size to use for chunk storage. The actual size is 2^(SEGMENT_SIZE)
     */
    private final int SEGMENT_SIZE = 8;
    /**
     * The timeout for the chunk storage in ms. If the store isn't accessed within that time, it can be automatically shutdown
     */
    public static final int TIMEOUT = 30000;
    private final File regionDirectory;
    private final ConcurrentHashMap<String, BAAWrapper> cache = new ConcurrentHashMap<>();
    private final TimeoutThread timeoutThread;

    public RegionFileManager(File worldDirectory) {
        this(worldDirectory, "region");
    }

    public RegionFileManager(File worldDirectory, String prefix) {
        this.regionDirectory = new File(worldDirectory, prefix);
        this.regionDirectory.mkdirs();
        this.timeoutThread = new TimeoutThread(worldDirectory);
        this.timeoutThread.start();
    }

    public BAAWrapper getBAAWrapper(int rx, int ry, int rz) {
        String filename = getFilename(rx, ry, rz);
        BAAWrapper regionFile = cache.get(filename);
        if (regionFile != null) {
            return regionFile;
        }
        File file = new File(regionDirectory, filename);
        regionFile = new BAAWrapper(file, SEGMENT_SIZE, FlowRegion.CHUNKS.VOLUME, TIMEOUT);
        BAAWrapper oldRegionFile = cache.putIfAbsent(filename, regionFile);
        if (oldRegionFile != null) {
            return oldRegionFile;
        }
        return regionFile;
    }

    /**
     * Gets the DataOutputStream corresponding to a given Chunk Snapshot.<br> <br> WARNING: This block will be locked until the stream is closed
     *
     * @param c the chunk snapshot
     * @return the DataOutputStream
     */
    public OutputStream getChunkOutputStream(ChunkSnapshot c) {
        int rx = c.getX() >> Region.CHUNKS.BITS;
        int ry = c.getY() >> Region.CHUNKS.BITS;
        int rz = c.getZ() >> Region.CHUNKS.BITS;
        return getBAAWrapper(rx, ry, rz).getBlockOutputStream(FlowRegion.getChunkKey(c.getX(), c.getY(), c.getZ()));
    }

    public void stopTimeoutThread() {
        timeoutThread.interrupt();
    }

    public void closeAll() {
        timeoutThread.interrupt();
        try {
            timeoutThread.join();
        } catch (InterruptedException ie) {
            Flow.getLogger().info("Interrupted when trying to stop RegionFileManager timeout thread");
        }
        for (BAAWrapper regionFile : cache.values()) {
            if (!regionFile.attemptClose()) {
                Flow.getLogger().info("Unable to close region file " + regionFile.getFilename());
            }
        }
    }

    private static String getFilename(int rx, int ry, int rz) {
        return "reg" + rx + "_" + ry + "_" + rz + ".spr";
    }

    private class TimeoutThread extends Thread {
        public TimeoutThread(File worldDirectory) {
            super("Region File Manager Timeout Thread - " + worldDirectory.getPath());
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                int files = cache.size();
                if (files <= 0) {
                    try {
                        Thread.sleep(TIMEOUT >> 1);
                    } catch (InterruptedException ie) {
                        return;
                    }
                    continue;
                }
                int cnt = 0;
                long start = System.currentTimeMillis();
                for (BAAWrapper regionFile : cache.values()) {
                    regionFile.timeoutCheck();
                    cnt++;
                    long currentTime = System.currentTimeMillis();
                    long expiredTime = currentTime - start;
                    long idealTime = (cnt * ((long) TIMEOUT)) / files / 2;
                    long excessTime = idealTime - expiredTime;
                    if (excessTime > 0) {
                        try {
                            Thread.sleep(excessTime);
                        } catch (InterruptedException ie) {
                            return;
                        }
                    } else if (isInterrupted()) {
                        return;
                    }
                }
            }
        }
    }
}
