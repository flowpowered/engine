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

import com.flowpowered.math.vector.Vector2f;
import org.spout.renderer.api.data.Uniform;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Vector2ArrayUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.util.CausticUtil;

public class ShadowMappingEffect {
    private final int kernelSize;
    private final Vector2f[] kernel;
    private final Vector2f noiseScale;
    private final Texture noiseTexture;
    private final float bias;
    private final float radius;

    public ShadowMappingEffect(GLFactory glFactory, Vector2f resolution, int kernelSize, int noiseSize, float bias, float radius) {
        this.kernelSize = kernelSize;
        this.kernel = new Vector2f[kernelSize];
        this.noiseScale = resolution.div(noiseSize);
        this.noiseTexture = glFactory.createTexture();
        this.bias = bias;
        this.radius = radius;
        // Generate the kernel
        final Random random = new Random();
        for (int i = 0; i < kernelSize; i++) {
            // Create a set of random unit vectors
            kernel[i] = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
        }
        // Generate the noise texture
        final int noiseTextureSize = noiseSize * noiseSize;
        final ByteBuffer noiseTextureBuffer = CausticUtil.createByteBuffer(noiseTextureSize * 3);
        for (int i = 0; i < noiseTextureSize; i++) {
            // Random unit vectors around the z axis
            Vector2f noise = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
            // Encode to unsigned byte, and place in buffer
            noise = noise.mul(128).add(128, 128);
            noiseTextureBuffer.put((byte) (noise.getFloorX() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorY() & 0xff));
            noiseTextureBuffer.put((byte) 0);
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
                new Vector2ArrayUniform("kernel", kernel),
                new Vector2Uniform("noiseScale", noiseScale),
                new FloatUniform("bias", bias),
                new FloatUniform("radius", radius)
        };
    }
}
