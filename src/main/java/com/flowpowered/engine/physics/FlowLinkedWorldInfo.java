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
package com.flowpowered.engine.physics;

import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.engine.geo.region.FlowRegion;
import com.flowpowered.engine.util.math.ReactConverter;
import org.spout.physics.body.RigidBody;
import org.spout.physics.collision.shape.CollisionShape;
import org.spout.physics.engine.Material;
import org.spout.physics.engine.linked.LinkedWorldInfo;
import org.spout.physics.math.Matrix3x3;
import org.spout.physics.math.Transform;

public final class FlowLinkedWorldInfo implements LinkedWorldInfo {
    private FlowRegion region;

    public FlowLinkedWorldInfo(FlowRegion region) {
        this.region = region;
    }

    @Override
    public RigidBody getBody(int x, int y, int z) {
        // TODO: fix this
        if (true) return null;
        final Block block = region.getBlock(x - 0.5f, y - 0.5f, z - 0.5f);
        final BlockMaterial material = block.getMaterial();
        final CollisionShape shape = material.getShape();
        if (shape == null) {
            return null;
        }
        final Matrix3x3 inertiaTensorLocal = new Matrix3x3();
        final float mass = material.getMass();
        shape.computeLocalInertiaTensor(inertiaTensorLocal, mass);
        final RigidBody body;
        body = new RigidBody(
                new Transform(ReactConverter.toReactVector3(x + 0.5f, y + 0.5f, z + 0.5f), ReactConverter.toReactQuaternion(0, 0, 0, 1)),
                mass,
                inertiaTensorLocal,
                shape, region.getDynamicsWorld().getNextFreeID());
        body.enableMotion(false);
        body.enableCollision(!material.isGhost());
        body.setMaterial(new Material(material.getRestitution(), material.getFriction()));
        return body;
    }
}
