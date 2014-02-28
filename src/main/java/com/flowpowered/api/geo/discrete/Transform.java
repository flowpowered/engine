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
package com.flowpowered.api.geo.discrete;

import java.io.Serializable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.flowpowered.commons.StringUtil;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector3f;

import org.apache.commons.lang3.Validate;

public final class Transform implements Serializable {
    private static final long serialVersionUID = 2L;
    public static final Transform INVALID = new Transform(Point.INVALID, Quaternionf.IDENTITY, Vector3f.ONE);
    private final Point position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    public Transform(Transform transform) {
        this(transform.position, transform.rotation, transform.scale);
    }

    public Transform(Point position, Quaternionf rotation, Vector3f scale) {
        Validate.notNull(position, "Position cannot be null!");
        Validate.notNull(rotation, "Rotation cannot be null!");
        Validate.notNull(scale, "Scale cannot be null!");
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public Point getPosition() {
        return position;
    }

    public Transform withPosition(Point position) {
        return new Transform(position, rotation, scale);
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public Transform withRotation(Quaternionf rotation) {
        return new Transform(position, rotation, scale);
    }

    public Vector3f getScale() {
        return scale;
    }

    public Transform withScale(Vector3f scale) {
        return new Transform(position, rotation, scale);
    }

    public Transform translated(float x, float y, float z) {
        return translated(new Vector3f(x, y, z));
    }

    public Transform translated(Vector3f offset) {
        return new Transform(new Point(position.getWorld(), position.getVector().add(offset)), rotation, scale);
    }

    public Transform rotated(Quaternionf offset) {
        return new Transform(position, offset.mul(rotation), scale);
    }

    public Transform scaled(Vector3f offset) {
        return new Transform(position, rotation, scale.add(offset));
    }

    public Transform translatedAndWithRotation(Vector3f offset, Quaternionf rotation) {
        return new Transform(new Point(position.getWorld(), position.getVector().add(offset)), rotation, scale);
    }

    /**
     * Gets a String representation of this transform
     * <p/>
     * Note: unsafe, could return torn values
     *
     * @return the string
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + StringUtil.toString(position, rotation, scale);
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder(41, 63);
        builder.append(position).append(rotation).append(scale);
        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Transform)) {
            return false;
        }
        Transform t = (Transform) other;
        return position.equals(t.position) && rotation.equals(t.rotation) && scale.equals(t.scale);
    }

    /**
     * Returns the 4x4 matrix that represents this transform object
     */
    public Matrix4f toMatrix() {
        Matrix4f translate = Matrix4f.createTranslation(getPosition().getVector());
        Matrix4f rotate = Matrix4f.createRotation(getRotation());
        Matrix4f scale = Matrix4f.createScaling(getScale().toVector4(1));
        return scale.mul(rotate).mul(translate);
    }

    /**
     * Returns a unit vector that points in the forward direction of this transform
     */
    public Vector3f forwardVector() {
        return Matrix3f.createRotation(getRotation()).transform(Vector3f.FORWARD);
    }

    /**
     * Returns a unit vector that points right in relation to this transform
     */
    public Vector3f rightVector() {
        return Matrix3f.createRotation(getRotation()).transform(Vector3f.RIGHT);
    }

    /**
     * Returns a unit vector that points up in relation to this transform
     */
    public Vector3f upVector() {
        return Matrix3f.createRotation(getRotation()).transform(Vector3f.UP);
    }

    /**
     * Returns if this Transform is "valid" <p> Invalid is defined by Position, {@link Point}, of the transform equaling {@link Point#invalid}, Rotation, {@link com.flowpowered.math.imaginary.Quaternionf}, of the transform equaling
     * {@link com.flowpowered.math.imaginary.Quaternionf#IDENTITY}, and Scale, {@link com.flowpowered.math.vector.Vector3f}, equaling {@link com.flowpowered.math.vector.Vector3f#ONE}.
     *
     * @return True if valid, false if not
     */
    public boolean isValid() {
        return !INVALID.equals(this);
    }
}
