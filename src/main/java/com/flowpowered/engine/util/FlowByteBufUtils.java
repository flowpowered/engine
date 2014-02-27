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
package com.flowpowered.engine.util;

import java.util.UUID;

import com.flowpowered.api.Client;
import com.flowpowered.api.Flow;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import io.netty.buffer.ByteBuf;

public class FlowByteBufUtils {
	public static Transform readTransform(ByteBuf buffer) {
		Point position = readPoint(buffer);
		Quaternionf rotation = readQuaternion(buffer);
		Vector3f scale = readVector3(buffer);
		return new Transform(position, rotation, scale);
	}

	public static void writeTransform(ByteBuf buffer, Transform transform) {
		writePoint(buffer, transform.getPosition());
		writeQuaternion(buffer, transform.getRotation());
		writeVector3(buffer, transform.getScale());
	}

	public static Vector3f readVector3(ByteBuf buffer) {
		final float x = buffer.readFloat();
		final float y = buffer.readFloat();
		final float z = buffer.readFloat();
		return new Vector3f(x, y, z);
	}

	public static void writeVector3(ByteBuf buffer, Vector3f vec) {
		buffer.writeFloat(vec.getX());
		buffer.writeFloat(vec.getY());
		buffer.writeFloat(vec.getZ());
	}

	public static UUID readUUID(ByteBuf buffer) {
		final long lsb = buffer.readLong();
		final long msb = buffer.readLong();
		return new UUID(msb, lsb);
	}

	public static void writeUUID(ByteBuf buffer, UUID uuid) {
		buffer.writeLong(uuid.getLeastSignificantBits());
		buffer.writeLong(uuid.getMostSignificantBits());
	}

    // TODO: pass in engine?
	public static Point readPoint(ByteBuf buffer) {
		UUID uuid = readUUID(buffer);
		World world = ((Client) Flow.getEngine()).getWorld();
		if (world == null) {
			throw new IllegalArgumentException("Unknown world with UUID " + uuid);
		}

		final float x = buffer.readFloat();
		final float y = buffer.readFloat();
		final float z = buffer.readFloat();
		return new Point(world, x, y, z);
	}

	public static void writePoint(ByteBuf buffer, Point vec) {
		writeUUID(buffer, vec.getWorld().getUID());
		buffer.writeFloat(vec.getBlockX());
		buffer.writeFloat(vec.getBlockY());
		buffer.writeFloat(vec.getBlockZ());
	}

	public static Quaternionf readQuaternion(ByteBuf buffer) {
		final float x = buffer.readFloat();
		final float y = buffer.readFloat();
		final float z = buffer.readFloat();
		final float w = buffer.readFloat();
		return new Quaternionf(x, y, z, w);
	}

	public static void writeQuaternion(ByteBuf buffer, Quaternionf quaternion) {
		buffer.writeFloat(quaternion.getX());
		buffer.writeFloat(quaternion.getY());
		buffer.writeFloat(quaternion.getZ());
		buffer.writeFloat(quaternion.getW());
	}
}
