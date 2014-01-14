/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spoutcraft <http://spoutcraft.org/>
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
package org.spout.engine.render.effect;

import java.nio.ByteBuffer;
import java.util.Random;

import org.spout.math.GenericMath;
import org.spout.math.vector.Vector2f;
import org.spout.math.vector.Vector3f;
import org.spout.renderer.api.data.Uniform;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.Uniform.Vector3ArrayUniform;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.util.CausticUtil;

public class SSAOEffect {
    private final int kernelSize;
    private final Vector3f[] kernel;
    private final float radius;
    private final float threshold;
    private final Vector2f noiseScale;
    private final Texture noiseTexture;
    private final float power;

    public SSAOEffect(GLFactory glFactory, Vector2f resolution, int kernelSize, int noiseSize, float radius, float threshold, float power) {
        this.kernelSize = kernelSize;
        this.kernel = new Vector3f[kernelSize];
        this.radius = radius;
        this.threshold = threshold;
        this.noiseScale = resolution.div(noiseSize);
        this.noiseTexture = glFactory.createTexture();
        this.power = power;
        // Generate the kernel
        final Random random = new Random();
        for (int i = 0; i < kernelSize; i++) {
            float scale = (float) i / kernelSize;
            scale = GenericMath.lerp(threshold, 1, scale * scale);
            // Create a set of random unit vectors inside a hemisphere
            // The vectors are scaled so that the amount falls of as we get further away from the center
            kernel[i] = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat()).normalize().mul(scale);
        }
        // Generate the noise texture
        final int noiseTextureSize = noiseSize * noiseSize;
        final ByteBuffer noiseTextureBuffer = CausticUtil.createByteBuffer(noiseTextureSize * 3);
        for (int i = 0; i < noiseTextureSize; i++) {
            // Random unit vectors around the z axis
            Vector3f noise = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, 0).normalize();
            // Encode to unsigned byte, and place in buffer
            noise = noise.mul(128).add(128, 128, 128);
            noiseTextureBuffer.put((byte) (noise.getFloorX() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorY() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorZ() & 0xff));
        }
        noiseTexture.setFormat(Format.RGB);
        noiseTexture.setInternalFormat(InternalFormat.RGB8);
        noiseTextureBuffer.flip();
        noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
        noiseTexture.create();
    }

    public void dispose() {
        noiseTexture.destroy();
    }

    public Texture getNoiseTexture() {
        return noiseTexture;
    }

    public Uniform[] getUniforms() {
        return new Uniform[]{
                new IntUniform("kernelSize", kernelSize),
                new Vector3ArrayUniform("kernel", kernel),
                new FloatUniform("radius", radius),
                new FloatUniform("threshold", threshold),
                new Vector2Uniform("noiseScale", noiseScale),
                new FloatUniform("power", power)
        };
    }
}
