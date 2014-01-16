package org.spout.engine.scheduler.render;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.flowpowered.commons.ticking.TickingElement;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import org.spout.api.Client;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.discrete.Point;
import org.spout.api.geo.discrete.Transform;
import org.spout.api.material.block.BlockFace;
import org.spout.api.material.block.BlockFaces;
import org.spout.engine.SpoutSingleplayer;
import org.spout.engine.geo.snapshot.ChunkSnapshot;
import org.spout.engine.geo.snapshot.RegionSnapshot;
import org.spout.engine.geo.snapshot.WorldSnapshot;
import org.spout.engine.geo.world.SpoutWorld;
import org.spout.engine.render.mesher.ParallelChunkMesher;
import org.spout.engine.render.mesher.StandardChunkMesher;
import org.spout.engine.render.SpoutRenderer;
import org.spout.engine.render.model.ChunkModel;
import org.spout.engine.scheduler.SpoutScheduler;
import org.spout.engine.scheduler.input.InputThread;
import org.spout.engine.scheduler.input.KeyboardEvent;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLVersioned;
import org.spout.renderer.api.data.Color;

public class RenderThread extends TickingElement {
    private final Client client;
    private final SpoutScheduler scheduler;
    private final SpoutRenderer renderer;
    private final InputThread input;
    private final ParallelChunkMesher mesher;
    // TEST CODE
    private final Map<Vector3i, ChunkModel> chunkModels = new HashMap<>();
    private long worldLastUpdateNumber;
    private final TObjectLongMap<Vector3i> chunkLastUpdateNumbers = new TObjectLongHashMap<>();

    public RenderThread(Client client, SpoutScheduler scheduler) {
        super("RenderThread", 60);
        this.client = client;
        this.scheduler = scheduler;
        this.renderer = new SpoutRenderer();
        this.input = scheduler.getInputThread();
        this.mesher = new ParallelChunkMesher(renderer, new StandardChunkMesher());
    }

    @Override
    public void onStart() {
        renderer.setGLVersion(GLVersioned.GLVersion.GL30);
        renderer.getCamera().setPosition(new Vector3f(0, 5, 0));
        renderer.setSolidColor(new Color(0, 200, 0));
        renderer.init();

        input.subscribeToKeyboard();
        input.getKeyboardQueue().add(new KeyboardEvent(' ', Keyboard.KEY_ESCAPE, true, 1));
    }

    @Override
    public void onStop() {
        renderer.dispose();
    }

    @Override
    public void onTick(long dt) {
        if (Display.isCloseRequested()) {
            scheduler.stop();
        }
        handleInput(dt / 1e9f);
        updateChunkModels(((SpoutWorld) client.getWorld()).getSnapshot());
        updateLight(client.getWorld().getAge());
        renderer.render();
    }

    public SpoutRenderer getRenderer() {
        return renderer;
    }

    public ParallelChunkMesher getMesher() {
        return mesher;
    }

    // TODO: not thread safe
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
        final Map<Vector3i, RegionSnapshot> regions = world.getRegions();
        for (RegionSnapshot region : regions.values()) {
            // Remove chunks we don't need anymore
            for (Iterator<Map.Entry<Vector3i, ChunkModel>> iterator = chunkModels.entrySet().iterator(); iterator.hasNext();) {
                final Map.Entry<Vector3i, ChunkModel> chunkModel = iterator.next();
                final Vector3i position = chunkModel.getKey();
                // If a model is not in the world chunk collection, we remove
                if (region.getChunk(position) == null) {
                    final ChunkModel model = chunkModel.getValue();
                    // Remove the model, destroying it
                    removeChunkModel(model, true);
                    // Finally, remove the chunk from the collections
                    iterator.remove();
                    chunkLastUpdateNumbers.remove(position);
                }
            }
            // Next go through all the chunks, and update the chunks that are out of date
            for (ChunkSnapshot chunk : region.getChunks()) {
                if (chunk == null) {
                    continue;
                }
                // If the chunk model is out of date
                if (chunk.getUpdateNumber() > chunkLastUpdateNumbers.get(chunk.getPosition())) {
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
        if (renderer.getModels().size() > chunkModels.size()) {
            System.out.println("There are more models in the renderer (" + renderer.getModels().size() + ") than there are chunk models " + chunkModels.size() + "), leak?");
        }
    }

    private void addChunkModel(ChunkSnapshot chunk, ChunkModel previous) {
        final ChunkModel model = mesher.queue(chunk);
        final Vector3i position = chunk.getPosition();
        model.setPosition(position.mul(16).toFloat());
        model.setRotation(Quaternionf.IDENTITY);
        // The previous model is kept to prevent frames with missing chunks because they're being meshed
        model.setPrevious(previous);
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
                // If the mouse has just been re-grabbed, ensure that movement when not grabbed will ignored
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
            final Camera camera = renderer.getCamera();
            final Vector3f right = camera.getRight();
            final Vector3f up = camera.getUp();
            final Vector3f forward = camera.getForward();
            Vector3f position = camera.getPosition();
            final float speed = CAMERA_SPEED * 60 * dt;
            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                position = position.add(forward.mul(speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                position = position.add(forward.mul(-speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                position = position.add(right.mul(-speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                position = position.add(right.mul(speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                position = position.add(up.mul(speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                position = position.add(up.mul(-speed));
            }
            Transform transform = ((SpoutSingleplayer) client).getTestEntity().getPhysics().getSnapshottedTransform();
            transform.setPosition(new Point(position, transform.getPosition().getWorld()));
            camera.setPosition(position);
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
                        renderer.saveScreenshot(new File(""));
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
        renderer.getCamera().setRotation(pitch.mul(yaw));
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
        final Vector3f direction = new Vector3f(0, -Math.sin(lightAngle), -Math.cos(lightAngle));
        final Vector3f position = renderer.getCamera().getPosition();
        renderer.updateLight(direction, new Vector3f(position.getX(), 0, position.getZ()), SHADOWED_CHUNKS);
        // TODO: lower light intensity at night
    }

}