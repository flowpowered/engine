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
package com.flowpowered.api.io.nbt;

import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;
import org.spout.nbt.CompoundMap;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.Tag;

public class TransformTag extends CompoundTag {
	public TransformTag(String name, Transform t) {
		this(name, t.getPosition().getVector(), t.getRotation(), t.getScale());
	}

	public TransformTag(String name, float px, float py, float pz, float qx, float qy, float qz, float qw, float sx, float sy, float sz) {
		this(name, new Vector3f(px, py, pz), new Quaternionf(qx, qy, qz, qw), new Vector3f(sx, sy, sz));
	}

	public TransformTag(String name, Vector3f p, Quaternionf q, Vector3f s) {
		super(name, toMap(p, q, s));
	}

	private static CompoundMap toMap(Vector3f p, Quaternionf q, Vector3f s) {
		CompoundMap map = new CompoundMap();
		map.put(new Vector3Tag("pos", p));
		map.put(new QuaternionTag("rot", q));
		map.put(new Vector3Tag("scale", s));
		return map;
	}

	public static Transform getValue(World w, Tag<?> tag) {
		try {
			return getValue(w, (CompoundTag) tag);
		} catch (ClassCastException e) {
			return null;
		}
	}

	public static Transform getValue(World w, CompoundTag map) {
		if (map == null || w == null) {
			return null;
		}
		return getValue(w, map.getValue());
	}

	public static Transform getValue(World w, CompoundMap map) {
		if (map == null || w == null) {
			return null;
		}
		Vector3f pVector = Vector3Tag.getValue(map.get("pos"));

		Quaternionf r = QuaternionTag.getValue(map.get("rot"));

		Vector3f s = Vector3Tag.getValue(map.get("scale"));

		if (pVector == null || r == null || s == null) {
			return null;
		}

		Point p = new Point(w, pVector);

		return new Transform(p, r, s);
	}
}
