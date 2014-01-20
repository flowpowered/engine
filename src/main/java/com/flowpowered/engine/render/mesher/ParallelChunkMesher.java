/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spoutcraft <http://spoutcraft.org/>
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
package com.flowpowered.engine.render.mesher;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.flowpowered.api.model.mesher.ChunkMesher;
import com.flowpowered.api.model.mesh.Mesh;
import com.flowpowered.engine.geo.snapshot.ChunkSnapshot;
import com.flowpowered.engine.geo.snapshot.ChunkSnapshotGroup;
import com.flowpowered.engine.render.SpoutRenderer;
import com.flowpowered.engine.render.model.ChunkModel;
import com.flowpowered.engine.scheduler.MarkedNamedThreadFactory;
import com.flowpowered.engine.util.thread.LoggingThreadPoolExecutor;
import org.spout.renderer.api.data.VertexData;

/**
 * Meshes chunks in parallel. Returns chunk models which may not be rendered when {@link org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel#render()} is called, this is happens when
 * the meshing is in progress. Parallelism is achieved using a {@link java.util.concurrent.ForkJoinPool} with default thread count. Chunks are meshed using the provided {@link
 * org.spoutcraft.client.nterface.mesh.ChunkMesher}. An optional {@link org.spoutcraft.client.nterface.Interface} can be passed to the constructor for chunk culling.
 *
 * @see org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel
 */
public class ParallelChunkMesher {
    private final SpoutRenderer renderer;
    private final ChunkMesher mesher;
    private final ThreadPoolExecutor executor;

    /**
     * Constructs a new parallel chunk mesher from the actual mesher.
     *
     * @param mesher The chunk mesher
     */
    public ParallelChunkMesher(SpoutRenderer renderer, ChunkMesher mesher) {
        this.renderer = renderer;
        this.mesher = mesher;
        this.executor = LoggingThreadPoolExecutor.newFixedThreadExecutorWithMarkedName(4, "ParallelChunkMesher");
        executor.setKeepAliveTime(60, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);
    }

    /**
     * Queues a chunk to be meshed, returning a chunk model which can be used normally. The chunk model will actually only renderer the chunk once meshing it complete.
     *
     * @param chunk The chunk to mesh
     * @return The chunk's model
     */
    public ChunkModel queue(ChunkSnapshot chunk) {
        return new ChunkModel(renderer, chunk.getPosition(), executor.submit(new ChunkMeshTask(chunk)));
    }

    /**
     * Shuts down the executor used for meshing, cancelling any meshing pending or active.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    private class ChunkMeshTask implements Callable<VertexData> {
        private final ChunkSnapshot toMesh;

        private ChunkMeshTask(ChunkSnapshot toMesh) {
            this.toMesh = toMesh;
        }

        @Override
        public VertexData call() {
            final Mesh mesh = mesher.mesh(new ChunkSnapshotGroup(toMesh));
            if (mesh.isEmpty()) {
                return null;
            }
            return mesh.build();
        }
    }
}
