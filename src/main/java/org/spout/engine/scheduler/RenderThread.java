package org.spout.engine.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.flowpowered.commons.ticking.TickingElement;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spout.api.Client;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.engine.geo.snapshot.ChunkSnapshot;
import org.spout.engine.geo.snapshot.RegionSnapshot;
import org.spout.engine.geo.snapshot.WorldSnapshot;
import org.spout.engine.geo.world.SpoutWorld;
import org.spout.engine.render.mesher.ParallelChunkMesher;
import org.spout.engine.render.mesher.StandardChunkMesher;
import org.spout.engine.render.SpoutRenderer;
import org.spout.engine.render.model.ChunkModel;
import org.spout.math.imaginary.Quaternionf;
import org.spout.math.vector.Vector3f;
import org.spout.math.vector.Vector3i;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLVersioned;
import org.spout.renderer.api.data.Color;

public class RenderThread extends TickingElement {
    private final Client client;
    private final SpoutScheduler scheduler;
    private final SpoutRenderer renderer;
    private final ParallelChunkMesher mesher;
    // TEST CODE
    private final List<ChunkModel> modelsToAdd = new ArrayList<>();
    private final Map<Vector3i, ChunkModel> chunkModels = new HashMap<>();
    private long worldLastUpdateNumber;
    private final TObjectLongMap<Vector3i> chunkLastUpdateNumbers = new TObjectLongHashMap<>();

    public RenderThread(Client client, SpoutScheduler scheduler) {
        super("RenderThread", 60);
        this.client = client;
        this.scheduler = scheduler;
        this.renderer = new SpoutRenderer();
        this.mesher = new ParallelChunkMesher(renderer, new StandardChunkMesher());
    }

    @Override
    public void onStart() {
        renderer.setGLVersion(GLVersioned.GLVersion.GL30);
        renderer.getCamera().setPosition(new Vector3f(0, 5, 0));
        renderer.setSolidColor(new Color(0, 200, 0));
        renderer.init();
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
        processInput(dt);
        updateChunkModels(((SpoutWorld) client.getWorld()).getSnapshot());
        updateLight(dt);
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

    public void addChunkModel(ChunkModel model) {
        modelsToAdd.add(model);
    }

    public void updateChunkModels() {
        for (ChunkModel model : modelsToAdd) {
            addChunkModelToRender(model);
        }
        modelsToAdd.clear();
    }

    private void addChunkModelToRender(ChunkModel model) {
        model.setRotation(Quaternionf.IDENTITY);
        renderer.addSolidModel(model);
    }

   // TEST CODE
    // TODO: properly handle user input
    private static float cameraPitch = 0;
    private static float cameraYaw = 0;
    private static float mouseSensitivity = 0.0000000001f;
    private static float cameraSpeed = 0.000000001f;
    private static boolean mouseGrabbed = true;

    private void processInput(float dt) {
        dt /= (1f / 60);
        final boolean mouseGrabbedBefore = mouseGrabbed;
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                switch (Keyboard.getEventKey()) {
                    case Keyboard.KEY_ESCAPE:
                        mouseGrabbed ^= true;
                        break;
                    case Keyboard.KEY_F2:
                        renderer.saveScreenshot(new File("screenies"));
                }
            }
        }
        final Camera camera = renderer.getCamera();
        if (Display.isActive()) {
            if (mouseGrabbed != mouseGrabbedBefore) {
                Mouse.setGrabbed(!mouseGrabbedBefore);
            }
            if (mouseGrabbed) {
                final float sensitivity = mouseSensitivity * dt;
                cameraPitch += Mouse.getDX() * sensitivity;
                cameraPitch %= 360;
                final Quaternionf pitch = Quaternionf.fromAngleDegAxis(cameraPitch, 0, 1, 0);
                cameraYaw += Mouse.getDY() * sensitivity;
                cameraYaw %= 360;
                final Quaternionf yaw = Quaternionf.fromAngleDegAxis(cameraYaw, 1, 0, 0);
                camera.setRotation(pitch.mul(yaw));
            }
            final Vector3f right = camera.getRight();
            final Vector3f up = camera.getUp();
            final Vector3f forward = camera.getForward();
            Vector3f position = camera.getPosition();
            final float speed = cameraSpeed * dt;
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
            camera.setPosition(position);
        }
    }

    private static final float LIGHT_ANGLE_LIMIT = (float) (Math.PI / 64);
    private static final Vector3f SHADOWED_CHUNKS = new Vector3f(Chunk.BLOCKS.SIZE * 4, 64, Chunk.BLOCKS.SIZE * 4);
    private void updateLight(long time) {
        time %= 1000 * 60 * 60 * 24;;
        double lightAngle;
        final double dayAngle = ((double) time / (1000 * 1000 * 60 * 60 * 24)) * Math.PI * 2;
        if (dayAngle < Math.PI) {
            lightAngle = dayAngle;
        } else {
            lightAngle = dayAngle - Math.PI;
        }
        lightAngle = lightAngle / Math.PI * (Math.PI - 2 * LIGHT_ANGLE_LIMIT) + LIGHT_ANGLE_LIMIT;
        final Vector3f direction = new Vector3f(0, -Math.sin(lightAngle), -Math.cos(lightAngle));
        final Vector3f position = renderer.getCamera().getPosition();
        renderer.updateLight(direction, new Vector3f(position.getX(), 0, position.getZ()), SHADOWED_CHUNKS);
        // TODO: lower light intensity at night
    }

}