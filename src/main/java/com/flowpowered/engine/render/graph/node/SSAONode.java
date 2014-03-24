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

package com.flowpowered.engine.render.graph.node;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import com.flowpowered.engine.render.graph.RenderGraph;
import com.flowpowered.math.GenericMath;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.Uniform.Vector3ArrayUniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.CausticUtil;
import org.spout.renderer.api.util.Rectangle;

public class SSAONode extends GraphNode {
    private final Texture noiseTexture;
    private final FrameBuffer frameBuffer;
    private final Texture occlusionsOutput;
    private final Material material;
    private final Pipeline pipeline;
    private final Rectangle outputSize = new Rectangle();
    private final Vector2Uniform projectionUniform = new Vector2Uniform("projection", Vector2f.ZERO);
    private final FloatUniform aspectRatioUniform = new FloatUniform("aspectRatio", 1);
    private final FloatUniform tanHalfFOVUniform = new FloatUniform("tanHalfFOV", 1);
    private final IntUniform kernelSizeUniform = new IntUniform("kernelSize", 0);
    private final Vector3ArrayUniform kernelUniform = new Vector3ArrayUniform("kernel", new Vector3f[]{});
    private final FloatUniform radiusUniform = new FloatUniform("radius", 0.5f);
    private final FloatUniform thresholdUniform = new FloatUniform("threshold", 0.15f);
    private final Vector2Uniform noiseScaleUniform = new Vector2Uniform("noiseScale", Vector2f.ONE);
    private final FloatUniform powerUniform = new FloatUniform("power", 2);

    public SSAONode(RenderGraph graph, String name) {
        super(graph, name);

        final Context context = graph.getContext();
        // Create the noise texture
        noiseTexture = context.newTexture();
        noiseTexture.create();
        noiseTexture.setFormat(InternalFormat.RGB8);
        noiseTexture.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        // Create the occlusions texture
        occlusionsOutput = context.newTexture();
        occlusionsOutput.create();
        occlusionsOutput.setFormat(InternalFormat.R8);
        occlusionsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        // Create the frame buffer
        frameBuffer = context.newFrameBuffer();
        frameBuffer.create();
        frameBuffer.attach(AttachmentPoint.COLOR0, occlusionsOutput);
        // Create the material
        material = new Material(graph.getProgram("ssao"));
        material.addTexture(2, noiseTexture);
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(projectionUniform);
        uniforms.add(tanHalfFOVUniform);
        uniforms.add(aspectRatioUniform);
        uniforms.add(kernelSizeUniform);
        uniforms.add(kernelUniform);
        uniforms.add(radiusUniform);
        uniforms.add(thresholdUniform);
        uniforms.add(noiseScaleUniform);
        uniforms.add(powerUniform);
        // Create the screen model
        final Model model = new Model(graph.getScreen(), material);
        // Create the pipeline
        pipeline = new PipelineBuilder().useViewPort(outputSize).bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
    }

    @Override
    public void destroy() {
        noiseTexture.destroy();
        frameBuffer.destroy();
        occlusionsOutput.destroy();
    }

    @Override
    public void render() {
        pipeline.run(graph.getContext());
    }

    @Setting
    public void setFieldOfView(float fieldOfView) {
        tanHalfFOVUniform.set(TrigMath.tan(Math.toRadians(fieldOfView) / 2));
    }

    @Setting
    public void setPlanes(Vector2f planes) {
        final float nearPlane = planes.getX();
        final float farPlane = planes.getY();
        projectionUniform.set(new Vector2f(farPlane / (farPlane - nearPlane), (-farPlane * nearPlane) / (farPlane - nearPlane)));
    }

    @Setting
    public void setKernelSize(int kernelSize) {
        // Generate the kernel
        final Vector3f[] kernel = new Vector3f[kernelSize];
        final Random random = new Random();
        final float threshold = thresholdUniform.get();
        for (int i = 0; i < kernelSize; i++) {
            float scale = (float) i / kernelSize;
            scale = GenericMath.lerp(threshold, 1, scale * scale);
            // Create a set of random unit vectors inside a hemisphere
            // The vectors are scaled so that the amount falls of as we get further away from the center
            kernel[i] = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat()).normalize().mul(scale);
        }
        // Update the uniforms
        kernelSizeUniform.set(kernelSize);
        kernelUniform.set(kernel);
    }

    @Setting
    public void setThreshold(float threshold) {
        if (threshold != thresholdUniform.get()) {
            thresholdUniform.set(threshold);
            // Recompute the kernel
            setKernelSize(kernelSizeUniform.get());
        }
    }

    @Setting
    public void setRadius(float radius) {
        radiusUniform.set(radius);
    }

    @Setting
    public void setNoiseSize(int noiseSize) {
        // Generate the noise texture data
        final Random random = new Random();
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
        // Update the uniform
        noiseScaleUniform.set(outputSize.getSize().toFloat().div(noiseSize));
        // Update the texture
        noiseTextureBuffer.flip();
        noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
    }

    @Setting
    public void setPower(float power) {
        powerUniform.set(power);
    }

    @Input("normals")
    public void setNormalsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(0, texture);
    }

    @Input("depths")
    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(1, texture);
        aspectRatioUniform.set((float) texture.getWidth() / texture.getHeight());
    }

    @Output("occlusions")
    public Texture getOcclusionsOutput() {
        return occlusionsOutput;
    }

    @Setting
    public void setOcclusionsSize(Vector2i size) {
        outputSize.setSize(size);
        occlusionsOutput.setImageData(null, size.getX(), size.getY());
    }
}