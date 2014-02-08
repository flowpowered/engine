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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import com.flowpowered.api.Client;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.commons.ViewFrustum;
import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.engine.FlowClient;
import com.flowpowered.engine.FlowSingleplayerImpl;
import com.flowpowered.engine.geo.snapshot.ChunkSnapshot;
import com.flowpowered.engine.geo.snapshot.RegionSnapshot;
import com.flowpowered.engine.geo.snapshot.WorldSnapshot;
import com.flowpowered.engine.render.FlowRenderer;
import com.flowpowered.engine.render.mesher.ParallelChunkMesher;
import com.flowpowered.engine.render.mesher.StandardChunkMesher;
import com.flowpowered.engine.render.model.ChunkModel;
import com.flowpowered.engine.scheduler.FlowScheduler;
import com.flowpowered.engine.scheduler.input.InputThread;
import com.flowpowered.engine.scheduler.input.KeyboardEvent;
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
    private final FlowClient client;
    private final FlowScheduler scheduler;
    private final FlowRenderer renderer;
    private final ViewFrustum frustum;
    private final InputThread input;
    private final ParallelChunkMesher mesher;
    // TEST CODE
    private final Map<Vector3i, ChunkModel> chunkModels = new HashMap<>();
    private long worldLastUpdateNumber;
    private final TObjectLongMap<Vector3i> chunkLastUpdateNumbers = new TObjectLongHashMap<>();

    public RenderThread(FlowClient client, FlowScheduler scheduler) {
        super("RenderThread", FPS);
        this.client = client;
        this.scheduler = scheduler;
        this.renderer = new FlowRenderer();
        this.frustum = new ViewFrustum();
        this.input = scheduler.getInputThread();
        this.mesher = new ParallelChunkMesher(this, new StandardChunkMesher());
    }

    @Override
    public void onStart() {
        renderer.setGLVersion(DEFAULT_VERSION);
        renderer.init(client.getScheduler());

        input.subscribeToKeyboard();
        input.getKeyboardQueue().add(new KeyboardEvent(' ', Keyboard.KEY_ESCAPE, true, 1));
        input.subscribeToMouse();
    }

    @Override
    public void onStop() {
        mesher.shutdown();
        // Updating with a null world will clear all models
        updateChunkModels(null);
        renderer.dispose();
    }

    @Override
    public void onTick(long dt) {
        handleInput(dt / 1e9f);
        updateChunkModels(client.getWorld().getSnapshot());
        updateLight(client.getWorld().getAge());
        renderer.render();
    }
    
    public FlowClient getEngine() {
        return client;
    }

    public FlowRenderer getRenderer() {
        return renderer;
    }

    public ParallelChunkMesher getMesher() {
        return mesher;
    }

    // TODO: not thread safe; WorldSnapshot could update in the middle of this
    private void updateChunkModels(WorldSnapshot world) {
        // If we have no world, remove all chunks
        if (world == null) {
            for (ChunkModel model : chunkModels.values()) {
                // Remove and destroy the model
                removeChunkModel(model, true);
            }
            chunkModels.clear();
            chunkLastUpdateNumbers.clear();
            worldLastUpdateNumber = 0;
            return;
        }
        // If the snapshot hasn't updated there's nothing to do
        if (world.getUpdateNumber() <= worldLastUpdateNumber) {
            return;
        }
        // Else, we need to update the chunk models
        // Remove chunks we don't need anymore
        for (Iterator<Map.Entry<Vector3i, ChunkModel>> iterator = chunkModels.entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry<Vector3i, ChunkModel> chunkModel = iterator.next();
            final Vector3i position = chunkModel.getKey();
            // If a model is not in the world chunk collection, we remove
            if (world.getChunk(position) == null) {
                final ChunkModel model = chunkModel.getValue();
                // Remove the model, destroying it
                removeChunkModel(model, true);
                // Finally, remove the chunk from the collections
                iterator.remove();
                chunkLastUpdateNumbers.remove(position);
            }
        }
        final Map<Vector3i, RegionSnapshot> regions = world.getRegions();
        for (RegionSnapshot region : regions.values()) {

            // Next go through all the chunks, and update the chunks that are out of date
            for (ChunkSnapshot chunk : region.getChunks()) {
                if (chunk == null) {
                    continue;
                }
                // If the chunk model is out of date and visible
                // TODO: only add models for visible chunks
                if (chunk.getUpdateNumber() > chunkLastUpdateNumbers.get(chunk.getPosition())
                        ) {//&& isChunkVisible(chunk.getPosition())) {
                    final Vector3i position = chunk.getPosition();
                    // If we have a previous model remove it to be replaced
                    final ChunkModel previous = chunkModels.get(position);
                    if (previous != null) {
                        // Don't destroy the model, we'll keep it to render until the new chunk is ready
                        removeChunkModel(previous, false);
                        // No need to remove from the collections, it will be replaced in the addChunkModel call
                    }
                    // Add the new model
                    addChunkModel(chunk, previous);
                }
            }
        }
        // Update the world update number
        worldLastUpdateNumber = world.getUpdateNumber();
        // Safety precautions
        if (renderer.getRenderModelsNode().getModels().size() > chunkModels.size()) {
            System.out.println("There are more models in the renderer (" + renderer.getRenderModelsNode().getModels().size() + ") than there are chunk models " + chunkModels.size() + "), leak?");
        }
    }

    private void addChunkModel(ChunkSnapshot chunk, ChunkModel previous) {
        final ChunkModel model = mesher.queue(chunk);
        final Vector3i position = chunk.getPosition();
        model.setPosition(position.mul(Chunk.BLOCKS.SIZE).toFloat());
        model.setRotation(Quaternionf.IDENTITY);
        // The previous model is kept to prevent frames with missing chunks because they're being meshed
        model.setPrevious(previous);
        renderer.addSolidModel(model);
        chunkModels.put(position, model);
        chunkLastUpdateNumbers.put(position, chunk.getUpdateNumber());
    }

    private void removeChunkModel(ChunkModel model, boolean destroy) {
        renderer.getRenderModelsNode().removeModel(model);
        if (destroy) {
            // TODO: recycle the vertex array?
            model.destroy();
        }
    }

    private static final float MOUSE_SENSITIVITY = 0.08f;
    private static final float CAMERA_SPEED = 0.2f;
    private float cameraPitch = 0;
    private float cameraYaw = 0;
    private int mouseX = 0;
    private int mouseY = 0;
    private boolean mouseGrabbed = false;

    private void handleInput(float dt) {
        // Store the old mouse grabbed state
        final boolean mouseGrabbedBefore = mouseGrabbed;
        // Handle keyboard events
        handleKeyboardEvents();
        // Handle the mouse and keyboard inputs, if the input is active
        if (input.isActive()) {
            // If the mouse grabbed state has changed from the keyboard events, update the mouse grabbed state
            if (mouseGrabbed != mouseGrabbedBefore) {
                input.setMouseGrabbed(mouseGrabbed);
                // If the mouse has just been re-grabbed, ensure that movement when not grabbed will be ignored
                if (mouseGrabbed) {
                    mouseX = input.getMouseX();
                    mouseY = input.getMouseY();
                }
            }
            // Handle the mouse input if it's been grabbed
            if (mouseGrabbed) {
                handleMouseInput(dt * 60);
            }
            // TODO: Update the camera position to match the player

            // TEST CODE!
            final Camera camera = renderer.getRenderModelsNode().getCamera();
            final Vector3f right = camera.getRight();
            final Vector3f up = camera.getUp();
            final Vector3f forward = camera.getForward();
            Vector3f translation = Vector3f.ZERO;
            final float speed = CAMERA_SPEED * 60 * dt;
            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                translation = translation.add(forward.mul(speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                translation = translation.add(forward.mul(-speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                translation = translation.add(right.mul(-speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                translation = translation.add(right.mul(speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                translation = translation.add(up.mul(speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                translation = translation.add(up.mul(-speed));
            }
            if (!translation.equals(Vector3f.ZERO)) {
                ((FlowSingleplayerImpl) client).getTestEntity().getPhysics().translate(translation);
            }
            // Update the frustrum to match the camera
            camera.setPosition(client.getPlayer().getTransformProvider().getTransform().getPosition().getVector());
            frustum.update(camera.getProjectionMatrix(), camera.getViewMatrix());
        }
    }

    private void handleKeyboardEvents() {
        final Queue<KeyboardEvent> keyboardEvents = input.getKeyboardQueue();
        while (!keyboardEvents.isEmpty()) {
            final KeyboardEvent event = keyboardEvents.poll();
            if (event.wasPressedDown()) {
                switch (event.getKey()) {
                    case Keyboard.KEY_ESCAPE:
                        mouseGrabbed ^= true;
                        break;
                    case Keyboard.KEY_F2:
                        renderer.saveScreenshot(null);
                }
            }
        }
    }

    private void handleMouseInput(float dt) {
        // Get the input
        // Calculate sensitivity adjusted to the FPS
        final float sensitivity = MOUSE_SENSITIVITY * dt;
        // Get the latest mouse x and y
        final int mouseX = input.getMouseX();
        final int mouseY = input.getMouseY();
        // Rotate the camera by the difference from the old and new mouse coordinates
        cameraPitch -= (mouseX - this.mouseX) * sensitivity;
        cameraPitch %= 360;
        final Quaternionf pitch = Quaternionf.fromAngleDegAxis(cameraPitch, 0, 1, 0);
        cameraYaw += (mouseY - this.mouseY) * sensitivity;
        cameraYaw %= 360;
        final Quaternionf yaw = Quaternionf.fromAngleDegAxis(cameraYaw, 1, 0, 0);
        // Set the new camera rotation
        renderer.getRenderModelsNode().getCamera().setRotation(pitch.mul(yaw));
        // Update the last mouse x and y
        this.mouseX = mouseX;
        this.mouseY = mouseY;
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
        renderer.updateLight(direction, frustum);
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
}