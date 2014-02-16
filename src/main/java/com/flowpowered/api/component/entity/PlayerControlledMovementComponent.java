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
package com.flowpowered.api.component.entity;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.flowpowered.api.input.InputSnapshot;
import com.flowpowered.api.input.MouseEvent;
import com.flowpowered.api.player.AbstractPlayer;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector3f;

public class PlayerControlledMovementComponent extends EntityComponent {
    private static final float SPEED = 20f;

    private AbstractPlayer controller;

    @Override
    public void onTick(float dt) {
        AbstractPlayer controller = this.controller;
        if (controller == null) {
            return;
        }
        final Vector3f right = getRight(getOwner().getPhysics().getRotation());
        final Vector3f up = getUp(getOwner().getPhysics().getRotation());
        final Vector3f forward = getForward(getOwner().getPhysics().getRotation());
        Vector3f translation = Vector3f.ZERO;
        List<InputSnapshot> input = controller.getInput();
        for (InputSnapshot snapshot : input) {
            if (!snapshot.isMouseGrabbed()) {
                continue;
            }
            final float speed = SPEED * snapshot.getDt();
            if (snapshot.isKeyDown(Keyboard.KEY_W)) {
                translation = translation.add(forward.mul(speed));
            }
            if (snapshot.isKeyDown(Keyboard.KEY_S)) {
                translation = translation.add(forward.mul(-speed));
            }
            if (snapshot.isKeyDown(Keyboard.KEY_A)) {
                translation = translation.add(right.mul(-speed));
            }
            if (snapshot.isKeyDown(Keyboard.KEY_D)) {
                translation = translation.add(right.mul(speed));
            }
            if (snapshot.isKeyDown(Keyboard.KEY_SPACE)) {
                translation = translation.add(up.mul(speed));
            }
            if (snapshot.isKeyDown(Keyboard.KEY_LSHIFT)) {
                translation = translation.add(up.mul(-speed));
            }
            if (!translation.equals(Vector3f.ZERO)) {
                getOwner().getPhysics().translate(translation);
            }
            handleMouseEvents(snapshot, dt);
        }
    }

    private static final float MOUSE_SENSITIVITY = 3f;
    private float cameraPitch = 0;
    private float cameraYaw = 0;

    private void handleMouseEvents(InputSnapshot snapshot, float dt) {
        for (MouseEvent event : snapshot.getMouseEvents()) {
            final int dx = event.getDX();
            final int dy = event.getDY();

            // Get the input
            // Calculate sensitivity adjusted to the FPS
            final float sensitivity = MOUSE_SENSITIVITY * dt;
            // Rotate the camera by the difference from the old and new mouse coordinates
            cameraPitch -= dx * sensitivity;
            cameraPitch %= 360;
            final Quaternionf pitch = Quaternionf.fromAngleDegAxis(cameraPitch, 0, 1, 0);
            cameraYaw += dy * sensitivity;
            cameraYaw %= 360;
            final Quaternionf yaw = Quaternionf.fromAngleDegAxis(cameraYaw, 1, 0, 0);
            // Set the new camera rotation
            getOwner().getPhysics().setRotation(pitch.mul(yaw));
        }
    }

    @Override
    public boolean canTick() {
        return controller != null;
    }

    public void setController(AbstractPlayer player) {
        this.controller = player;
    }

    // TODO: move to a better place

    /**
     * Gets the vector representing the right direction for the camera.
     *
     * @return The camera's right direction vector
     */
    public Vector3f getRight(Quaternionf rotation) {
        return toCamera(rotation, Vector3f.RIGHT);
    }

    /**
     * Gets the vector representing the up direction for the camera.
     *
     * @return The camera's up direction vector
     */
    public Vector3f getUp(Quaternionf rotation) {
        return toCamera(rotation, Vector3f.UP);
    }

    /**
     * Gets the vector representing the forward direction for the camera.
     *
     * @return The camera's forward direction vector
     */
    public Vector3f getForward(Quaternionf rotation) {
        return toCamera(rotation, Vector3f.FORWARD.negate());
    }

    private Vector3f toCamera(Quaternionf rotation, Vector3f v) {
        Matrix4f rotationMatrixInverse = Matrix4f.createRotation(rotation);
        return rotationMatrixInverse.transform(v.toVector4(1)).toVector3();
    }
}
