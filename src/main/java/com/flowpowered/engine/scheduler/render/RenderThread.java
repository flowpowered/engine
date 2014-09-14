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
package com.flowpowered.engine.scheduler.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.reference.ChunkReference;
import com.flowpowered.api.geo.reference.WorldReference;
import com.flowpowered.api.geo.snapshot.ChunkSnapshot;
import com.flowpowered.api.input.KeyboardEvent;
import com.flowpowered.commons.ViewFrustum;
import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.engine.FlowClient;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.geo.snapshot.FlowChunkSnapshot;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.render.FlowRenderer;
import com.flowpowered.engine.render.mesher.ParallelChunkMesher;
import com.flowpowered.engine.render.mesher.StandardChunkMesher;
import com.flowpowered.engine.render.model.ChunkModel;
import com.flowpowered.engine.scheduler.input.InputThread;
import com.flowpowered.engine.util.ClientObserver;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.lwjgl.input.Keyboard;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLVersioned.GLVersion;

public class RenderThread extends TickingElement {
    public static final int FPS = 60;
    public static final GLVersion DEFAULT_VERSION = GLVersion.GL32;
    private final FlowEngine engine;
    private final FlowClient client;
    private final FlowRenderer renderer;
    private final ViewFrustum frustum;
    private final InputThread input;
    private final ParallelChunkMesher mesher;
    // TEST CODE
    private final LinkedBlockingQueue<ChunkReference> toAdd = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ChunkReference> toRemove = new LinkedBlockingQueue<>();
    private final Map<Vector3i, ChunkSnapshot> chunks = new HashMap<>();
    private final Map<Vector3i, ChunkModel> chunkModels = new HashMap<>();
    private final TObjectLongMap<Vector3i> chunkLastUpdateNumbers = new TObjectLongHashMap<>();
    private final CountDownLatch intializedLatch = new CountDownLatch(1);
    private final ClientObserver observe;

    public RenderThread(FlowEngine engine, FlowClient client) {
        super("RenderThread", FPS);
        this.engine = engine;
        this.client = client;
        this.renderer = new FlowRenderer();
        this.frustum = new ViewFrustum();
        this.input = engine.getScheduler().getInputThread();
        this.mesher = new ParallelChunkMesher(this, new StandardChunkMesher());
        this.observe = new ClientObserver(engine, client);
    }

    @Override
    public void onStart() {
        renderer.setGLVersion(DEFAULT_VERSION);
        renderer.init(engine.getScheduler(), client);

        input.subscribeToKeyboard();
        input.getKeyboardQueue().add(new KeyboardEvent(Keyboard.KEY_ESCAPE, true));
        input.subscribeToMouse();
        intializedLatch.countDown();
    }

    @Override
    public void onStop() {
        mesher.shutdown();
        // Updating with a null world will clear all models
        clearChunkModels();
        renderer.dispose();
    }

    @Override
    public void onTick(long dt) {
        handleInput();
        updateCameraAndFrustrum();
        observe.update();
        WorldReference ref = client.getTransform().getPosition().getWorld();
        FlowWorld world = ref == null ? null : (FlowWorld) ref.get();
        updateChunkModels();
        updateLight(world == null ? 0 : world.getAge());
        renderer.render();
    }
    
    public FlowEngine getEngine() {
        return engine;
    }

    public FlowRenderer getRenderer() {
        return renderer;
    }

    public ParallelChunkMesher getMesher() {
        return mesher;
    }

    public void addChunks(Set<ChunkReference> chunks) {
        toAdd.addAll(chunks);
    }

    public void removeChunks(Set<ChunkReference> chunks) {
        toRemove.addAll(chunks);
    }

    private void clearChunkModels() {
        for (ChunkModel model : chunkModels.values()) {
            // Remove and destroy the model
            removeChunkModel(model, true);
        }
        chunkModels.clear();
        chunkLastUpdateNumbers.clear();
    }

    private void updateChunkModels() {
        Set<ChunkReference> removeChunks = new HashSet<>();
        toRemove.drainTo(removeChunks);
        for (ChunkReference ref : removeChunks) {
            final Vector3i blockBase = ref.getBase().getVector().toInt();
            final Vector3i position = new Vector3i(blockBase.getX() >> Chunk.BLOCKS.BITS, blockBase.getY() >> Chunk.BLOCKS.BITS, blockBase.getZ() >> Chunk.BLOCKS.BITS);
            chunks.remove(position);
            final ChunkModel model = chunkModels.remove(position);
            if (model == null) continue;
            // Remove the model, destroying it
            removeChunkModel(model, true);
            // Finally, remove the chunk from the collections
            chunkLastUpdateNumbers.remove(position);
        }


        Map<Vector3i, ChunkSnapshot> oldChunks = new HashMap<>(this.chunks);

        Set<ChunkReference> newChunks = new HashSet<>();
        toAdd.drainTo(newChunks);
        for (ChunkReference ref : newChunks) {
            FlowChunk chunk = (FlowChunk) ref.get();
            if (chunk == null) {
                throw new IllegalArgumentException("Can't give the renderer a chunk that unloads! This is the client!");
            }

            FlowChunkSnapshot snapshot = chunk.getSnapshot();
            final Vector3i position = snapshot.getPosition();

            // TODO: priority meshing? new meshes > old meshes; closer meshes > farther meshes
            // Add current chunk
            this.chunks.put(position, snapshot);
            addChunkModel(snapshot);
            chunkLastUpdateNumbers.put(position, snapshot.getUpdateNumber());

            // TODO: add this back in to make ChunkModels in render as efficient as possible
            // Remesh surrounding chunks
            //for (BlockFace f : BlockFaces.NESWBT) {
            //    Vector3i localPosition = position.add(f.getOffset());
            //    ChunkSnapshot old = oldChunks.get(localPosition);
            //    if (old != null) {
            //        chunkLastUpdateNumbers.put(localPosition, 0);
            //    }
            //}
        }

        for (ChunkSnapshot chunk : oldChunks.values()) {
            // If the chunk model is out of date and visible
            if (chunk.getUpdateNumber() > chunkLastUpdateNumbers.get(chunk.getPosition())) {
                // TODO: only add models for visible chunks
                // The problem is that if turn and new chunks are now visible, they won't be updated because they're part of a previous world update number
                // One possible way to reconcile this is update every partial update
                // The problem is that almost every update will probably be a partial update, meaning there is no point for the world update number
                // The downside with always add the model is more models in renderer
                //if (!isChunkVisible(chunk.getPosition())) {
                //    continue;
                //}
                // TODO: we need to check if the surrounding chunks need to be remeshed (should only be true if edge blocks change transparency)
                // If we can somehow get the exact faces of the chunk which are dirty, we can cut down on remeshing of surrounding chunks to about 1/6 best case to 3/6 worst case for one block change
                addChunkModel(chunk);
            }
        }
        // Safety precautions
        if (renderer.getRenderModelsNode().getAttribute("models", Collections.EMPTY_LIST).size() > chunkModels.size()) {
            System.out.println("There are more models in the renderer (" + renderer.getRenderModelsNode().getAttribute("models", Collections.EMPTY_LIST).size() + ") than there are chunk models " + chunkModels.size() + "), leak?");
        }
    }

    private void addChunkModel(ChunkSnapshot chunk) {
        final Vector3i position = chunk.getPosition();
        // If we have a previous model remove it to be replaced
        final ChunkModel previous = chunkModels.remove(position);
        if (previous != null) {
            removeChunkModel(previous, false);
        }
        // The previous model is kept to prevent frames with missing chunks because they're being meshed
        final ChunkModel model = mesher.queue(chunk, previous);
        model.setPosition(position.mul(Chunk.BLOCKS.SIZE).toFloat());
        model.setRotation(Quaternionf.IDENTITY);
        renderer.addSolidModel(model);
        chunkModels.put(position, model);
        chunkLastUpdateNumbers.put(position, chunk.getUpdateNumber());
    }

    private void removeChunkModel(ChunkModel model, boolean destroy) {
        renderer.removeModel(model);
        if (destroy) {
            // TODO: recycle the vertex array?
            model.destroy();
        }
    }

    private void handleInput() {
        // Handle keyboard events
        final Queue<KeyboardEvent> keyboardEvents = input.getKeyboardQueue();
        while (!keyboardEvents.isEmpty()) {
            final KeyboardEvent event = keyboardEvents.poll();
            if (event.wasPressedDown()) {
                switch (event.getKeyId()) {
                    case Keyboard.KEY_ESCAPE:
                        input.setMouseGrabbed(!input.isMouseGrabbed());
                        break;
                    case Keyboard.KEY_F2:
                        renderer.saveScreenshot(null);
                }
            }
        }
    }

    private void updateCameraAndFrustrum() {
        final Camera camera = renderer.getRenderModelsNode().getAttribute("camera");
        // Update camera to match client player's position
        Transform transform = client.getTransform();
        camera.setPosition(transform.getPosition().getVector());
        camera.setRotation(transform.getRotation());
        // Update the frustrum to match the camera
        frustum.update(camera.getProjectionMatrix(), camera.getViewMatrix());
    }

    private static final float PI = (float) TrigMath.PI;
    private static final float TWO_PI = 2 * PI;
    private static final float LIGHT_ANGLE_LIMIT = PI / 64;
    private static final Vector3f SHADOWED_CHUNKS = new Vector3f(Chunk.BLOCKS.SIZE * 4, 64, Chunk.BLOCKS.SIZE * 4);
    private static final long MILLIS_IN_A_DAY = 1000 * 60 * 60 * 24;
    private void updateLight(long time) {
        time %= MILLIS_IN_A_DAY;;
        double lightAngle;
        final double dayAngle = ((double) time / (MILLIS_IN_A_DAY)) * TWO_PI;
        if (dayAngle < Math.PI) {
            lightAngle = dayAngle;
        } else {
            lightAngle = dayAngle - PI;
        }
        lightAngle = lightAngle / PI * (PI - 2 * LIGHT_ANGLE_LIMIT) + LIGHT_ANGLE_LIMIT;
        final Vector3f direction = new Vector3f(0, -TrigMath.sin(lightAngle), -TrigMath.cos(lightAngle));
        renderer.updateLight(direction);
        // TODO: lower light intensity at night
    }

    private static final Vector3f[] CHUNK_VERTICES;
    static {
        CHUNK_VERTICES = new Vector3f[8];
        CHUNK_VERTICES[0] = new Vector3f(0, 0, Chunk.BLOCKS.SIZE);
        CHUNK_VERTICES[1] = new Vector3f(Chunk.BLOCKS.SIZE, 0, Chunk.BLOCKS.SIZE);
        CHUNK_VERTICES[2] = new Vector3f(Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE);
        CHUNK_VERTICES[3] = new Vector3f(0, Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE);
        CHUNK_VERTICES[4] = new Vector3f(0, 0, 0);
        CHUNK_VERTICES[5] = new Vector3f(Chunk.BLOCKS.SIZE, 0, 0);
        CHUNK_VERTICES[6] = new Vector3f(Chunk.BLOCKS.SIZE, Chunk.BLOCKS.SIZE, 0);
        CHUNK_VERTICES[7] = new Vector3f(0, Chunk.BLOCKS.SIZE, 0);
    }

    /**
     * Returns true if the chunk is visible, using the default chunk size and the position in world coordinates.
     *
     * @param position The position, in world coordinates
     * @return Whether or not the chunk is visible
     */
    public boolean isChunkVisible(Vector3i position) {
        return frustum.intersectsCuboid(CHUNK_VERTICES, position.getX(), position.getY(), position.getZ());
    }

    /**
     * Returns true if the chunk is visible, using the default chunk size and the position in world coordinates.
     *
     * @param position The position, in world coordinates
     * @return Whether or not the chunk is visible
     */
    public boolean isChunkVisible(Vector3f position) {
        // It's hard to look right
        // at the world baby
        // But here's my frustum
        // so cull me maybe?
        return frustum.intersectsCuboid(CHUNK_VERTICES, position);
    }

    public CountDownLatch getIntializedLatch() {
        return intializedLatch;
    }
}