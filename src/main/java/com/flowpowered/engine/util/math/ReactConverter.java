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
package com.flowpowered.engine.util.math;

import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.geo.reference.WorldReference;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;

public class ReactConverter {
    //Flow -> React
    public static org.spout.physics.math.Vector3 toReactVector3(final Vector3f flowVector3) {
        return new org.spout.physics.math.Vector3(flowVector3.getX(), flowVector3.getY(), flowVector3.getZ());
    }

    public static org.spout.physics.math.Vector3 toReactVector3(final float x, final float y, final float z) {
        return new org.spout.physics.math.Vector3(x, y, z);
    }

    public static org.spout.physics.math.Quaternion toReactQuaternion(final Quaternionf flowQuaternion) {
        return new org.spout.physics.math.Quaternion(flowQuaternion.getX(), flowQuaternion.getY(), flowQuaternion.getZ(), flowQuaternion.getW());
    }

    public static org.spout.physics.math.Quaternion toReactQuaternion(final float w, final float x, final float y, final float z) {
        return new org.spout.physics.math.Quaternion(x, y, z, w);
    }

    public static org.spout.physics.math.Transform toReactTransform(final Transform flowTransform) {
        return new org.spout.physics.math.Transform(toReactVector3(flowTransform.getPosition().getVector()), toReactQuaternion(flowTransform.getRotation()));
    }

    //React -> Flow
    public static Vector3f toFlowVector3(final org.spout.physics.math.Vector3 reactVector3) {
        return new Vector3f(reactVector3.getX(), reactVector3.getY(), reactVector3.getZ());
    }

    public static Quaternionf toFlowQuaternion(final org.spout.physics.math.Quaternion reactQuaternion) {
        return new Quaternionf(reactQuaternion.getX(), reactQuaternion.getY(), reactQuaternion.getZ(), reactQuaternion.getW());
    }

    public static Transform toFlowTransform(final org.spout.physics.math.Transform reactTransform, final WorldReference world, final Vector3f scale) {
        return new Transform(new Point(world, toFlowVector3(reactTransform.getPosition())), new Quaternionf(toFlowQuaternion(reactTransform.getOrientation())), scale);
    }
}