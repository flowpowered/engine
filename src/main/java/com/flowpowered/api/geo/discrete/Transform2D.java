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

import com.flowpowered.math.imaginary.Complexf;
import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.vector.Vector2f;

public class Transform2D {
	private Vector2f position;
	private Complexf rotation;
	private Vector2f scale;

	public Transform2D() {
		this(Vector2f.ZERO, Complexf.IDENTITY, Vector2f.ONE);
	}

	public Transform2D(Vector2f position, Complexf rotation, Vector2f scale) {
		this.position = position;
		this.rotation = rotation;
		this.scale = scale;
	}

	public void add(float x, float y) {
		setPosition(getPosition().add(x, y));
	}

	public void setPosition(float x, float y) {
		this.position = new Vector2f(x, y);
	}

	public void setPosition(Vector2f position) {
		this.position = position;
	}

	public Vector2f getPosition() {
		return position;
	}

	public void setScale(float scale) {
		this.scale = new Vector2f(scale, scale);
	}

	public void setScale(float scaleX, float scaleY) {
		this.scale = new Vector2f(scaleX, scaleY);
	}

	public void setScale(Vector2f scale) {
		this.scale = scale;
	}

	public Vector2f getScale() {
		return scale;
	}

	public void setRotation(float angle) {
		this.rotation = Complexf.fromAngleDeg(angle);
	}

	public void setRotation(Complexf rotation) {
		this.rotation = rotation;
	}

	public Complexf getRotation() {
		return rotation;
	}

	public Matrix3f toMatrix() {
		Matrix3f rotation = Matrix3f.createRotation(this.rotation);
		Matrix3f translation = Matrix3f.createTranslation(position);
		Matrix3f scale = Matrix3f.createScaling(this.scale.toVector3(1));
		return scale.mul(rotation).mul(translation);
	}
}
