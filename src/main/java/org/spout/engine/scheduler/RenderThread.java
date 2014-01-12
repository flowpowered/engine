package org.spout.engine.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.flowpowered.commons.ticking.TickingElement;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.spout.api.model.mesher.ParallelChunkMesher;
import org.spout.api.model.mesher.ParallelChunkMesher.ChunkModel;
import org.spout.api.model.mesher.StandardChunkMesher;
import org.spout.engine.geo.chunk.SpoutChunkSnapshotGroup;
import org.spout.engine.render.SpoutRenderer;
import org.spout.math.imaginary.Quaternionf;
import org.spout.math.vector.Vector3f;
import org.spout.math.vector.Vector3i;
import org.spout.renderer.Camera;
import org.spout.renderer.GLVersioned;
import org.spout.renderer.data.Color;

public class RenderThread extends TickingElement {
    private final SpoutScheduler scheduler;
    private final SpoutRenderer renderer;
    private final ParallelChunkMesher mesher;
    // TEST CODE
    private final List<ChunkModel> models = new ArrayList<>();

    public RenderThread(SpoutScheduler scheduler) {
        super("RenderThread", 60);
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
        updateChunkModels();
        renderer.render();
    }

    public SpoutRenderer getRenderer() {
        return renderer;
    }

    public ParallelChunkMesher getMesher() {
        return mesher;
    }

    public void addChunkModel(ChunkModel model) {
        models.add(model);
    }

    public void updateChunkModels() {
        for (ChunkModel model : models) {
            addChunkModelToRender(model);
        }
        models.clear();
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
                cameraPitch -= Mouse.getDX() * sensitivity;
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
                position = position.add(right.mul(speed));
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                position = position.add(right.mul(-speed));
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
}
