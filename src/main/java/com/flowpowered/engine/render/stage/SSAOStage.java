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
package com.flowpowered.engine.render.stage;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.engine.render.FlowRenderer;

import org.spout.renderer.api.Creatable;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.Uniform.Vector3ArrayUniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.CausticUtil;

public class SSAOStage extends Creatable {
    private final FlowRenderer renderer;
    private final Material material;
    private final Texture noiseTexture;
    private final FrameBuffer frameBuffer;
    private Texture normalsInput;
    private Texture depthsInput;
    private Texture occlusionsOutput;
    private Pipeline pipeline;
    private int kernelSize = 8;
    private float radius = 0.5f;
    private float threshold = 0.15f;
    private int noiseSize = 4;
    private float power = 2;

    public SSAOStage(FlowRenderer renderer) {
        this.renderer = renderer;
        material = new Material(renderer.getProgram("ssao"));
        final GLFactory glFactory = renderer.getGLFactory();
        noiseTexture = glFactory.createTexture();
        frameBuffer = glFactory.createFrameBuffer();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("SSAO stage has already been created");
        }
        // Generate the kernel
        final Vector3f[] kernel = new Vector3f[kernelSize];
        final Random random = new Random();
        for (int i = 0; i < kernelSize; i++) {
            float scale = (float) i / kernelSize;
            scale = GenericMath.lerp(threshold, 1, scale * scale);
            // Create a set of random unit vectors inside a hemisphere
            // The vectors are scaled so that the amount falls of as we get further away from the center
            kernel[i] = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat()).normalize().mul(scale);
        }
        // Generate the noise texture data
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
        // Create the texture
        noiseTexture.setFormat(Format.RGB);
        noiseTexture.setInternalFormat(InternalFormat.RGB8);
        noiseTextureBuffer.flip();
        noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
        noiseTexture.create();
        // Create the material
        material.addTexture(0, normalsInput);
        material.addTexture(1, depthsInput);
        material.addTexture(2, noiseTexture);
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(new Vector2Uniform("projection", FlowRenderer.PROJECTION));
        uniforms.add(new FloatUniform("tanHalfFOV", FlowRenderer.TAN_HALF_FOV));
        uniforms.add(new FloatUniform("aspectRatio", FlowRenderer.ASPECT_RATIO));
        uniforms.add(new IntUniform("kernelSize", kernelSize));
        uniforms.add(new Vector3ArrayUniform("kernel", kernel));
        uniforms.add(new FloatUniform("radius", radius));
        uniforms.add(new FloatUniform("threshold", threshold));
        uniforms.add(new Vector2Uniform("noiseScale",  new Vector2f(occlusionsOutput.getWidth(), occlusionsOutput.getHeight()).div(noiseSize)));
        uniforms.add(new FloatUniform("power", power));
        // Create the screen model
        final Model model = new Model(renderer.getScreen(), material);
        // Create the frame buffer
        frameBuffer.attach(AttachmentPoint.COLOR0, occlusionsOutput);
        frameBuffer.create();
        // Create the pipeline
        pipeline = new PipelineBuilder().bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
        // Update state to created
        super.create();
    }

    @Override
    public void destroy() {
        checkCreated();
        noiseTexture.destroy();
        frameBuffer.destroy();
        occlusionsOutput.destroy();
        super.destroy();
    }

    public void render() {
        checkCreated();
        pipeline.run(renderer.getContext());
    }

    public void setKernelSize(int kernelSize) {
        this.kernelSize = kernelSize;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public void setNoiseSize(int noiseSize) {
        this.noiseSize = noiseSize;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public void setNormalsInput(Texture texture) {
        texture.checkCreated();
        normalsInput = texture;
    }

    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        depthsInput = texture;
    }

    public void setOcclusionOutput(Texture texture) {
        texture.checkCreated();
        occlusionsOutput = texture;
    }

    public Texture getOcclusionsOutput() {
        return occlusionsOutput;
    }
}
