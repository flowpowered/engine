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
package org.spout.api.model.mesher;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.spout.engine.geo.snapshot.ChunkSnapshotGroup;
import org.spout.engine.render.SpoutRenderer;
import org.spout.math.vector.Vector3i;
import org.spout.renderer.data.VertexData;
import org.spout.renderer.gl.VertexArray;
import org.spout.renderer.model.Model;

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
        this.executor = new ThreadPoolExecutor(4, 4,
                                      60L, TimeUnit.SECONDS,
                                      new LinkedBlockingQueue<Runnable>());
        executor.allowCoreThreadTimeOut(true);
    }

    /**
     * Queues a chunk to be meshed, returning a chunk model which can be used normally. The chunk model will actually only renderer the chunk once meshing it complete.
     *
     * @param chunk The chunk to mesh
     * @return The chunk's model
     */
    public ChunkModel queue(ChunkSnapshotGroup chunk) {
        return new ChunkModel(chunk.getMiddle().getPosition(), executor.submit(new ChunkMeshTask(chunk)));
    }

    /**
     * Shuts down the executor used for meshing, cancelling any meshing pending or active.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    private class ChunkMeshTask implements Callable<VertexData> {
        private final ChunkSnapshotGroup toMesh;

        private ChunkMeshTask(ChunkSnapshotGroup toMesh) {
            this.toMesh = toMesh;
        }

        @Override
        public VertexData call() {
            final Mesh mesh = mesher.mesh(toMesh);
            if (mesh.isEmpty()) {
                return null;
            }
            return mesh.build();
        }
    }

    /**
     * In the case that meshing is occurring and that the chunk is not renderable, a previous model can be rendered instead. To use this feature, set the previous model using {@link
     * org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel#setPrevious(org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel)}. This previous model will be used until the
     * mesh becomes available. At this point, the previous model will be destroyed, and the new one rendered. When a model isn't needed anymore, you must call {@link
     * org.spoutcraft.client.nterface.mesh.ParallelChunkMesher.ChunkModel#destroy()} to dispose of it completely. This will also cancel the meshing if it's in progress, and destroy the previous model.
     * The chunk can also be automatically culled by passing an optional {@link org.spoutcraft.client.nterface.Interface} to the {@link org.spoutcraft.client.nterface.mesh.ParallelChunkMesher}
     * constructor.
     */
    public class ChunkModel extends Model {
        private Future<VertexData> mesh;
        private boolean complete = false;
        private ChunkModel previous;

        private ChunkModel(Vector3i position, Future<VertexData> mesh) {
            this.mesh = mesh;
            // TODO: replace magic number
            setPosition(position.mul(16).toFloat());
        }

        @Override
        public void render() {
            // If we have not received the mesh and it's done
            if (!complete && mesh.isDone()) {
                // Get the mesh
                final VertexData vertexData;
                try {
                    vertexData = mesh.get();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                mesh = null;
                // If the chunk mesher returned a mesh. It may not return one if the chunk has no mesh (completely invisible)
                if (vertexData != null) {
                    // Create the vertex array from the mesh
                    final VertexArray vertexArray = renderer.getGLFactory().createVertexArray();
                    vertexArray.setData(vertexData);
                    vertexArray.create();
                    // Set it for rendering
                    setVertexArray(vertexArray);
                }
                // Destroy and discard the previous model (if any), as it is now obsolete
                if (previous != null) {
                    previous.destroy();
                    previous = null;
                }
                // Set the model as complete
                complete = true;
            }
            if (!isVisible()) {
                return;
            }
            // If we have a vertex array, we can render
            if (complete) {
                // Only render if the model has a vertex array and we're visible
                if (getVertexArray() != null && isVisible()) {
                    super.render();
                }
            } else if (previous != null && isVisible()) {
                // Else, fall back on the previous model if we have one and we're visible
                previous.render();
            }
        }

        private boolean isVisible() {
            // It's hard to look right
            // at the world baby
            // But here's my frustum
            // so cull me maybe?
            return true;
            // TODO frustrum
        }

        /**
         * Sets the previous model to renderer until the updated one is ready.
         *
         * @param previous The previous model
         */
        public void setPrevious(ChunkModel previous) {
            this.previous = previous;
        }

        /**
         * Destroys the models, cancelling the meshing task if in progress, and the previous model (if any).
         */
        public void destroy() {
            // If we have a vertex array, destroy it
            if (complete) {
                if (getVertexArray() != null) {
                    getVertexArray().destroy();
                }
                complete = false;
            } else {
                // Else, the mesh is still in progress, cancel that
                mesh.cancel(false);
                mesh = null;
                // Also destroy and discard the previous model if we have one
                if (previous != null) {
                    previous.destroy();
                    previous = null;
                }
            }
        }

    }
}
