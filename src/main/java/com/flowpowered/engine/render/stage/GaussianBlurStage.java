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
package com.flowpowered.engine.render.stage;

import java.util.Arrays;

import org.spout.renderer.api.Creatable;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.BooleanUniform;
import org.spout.renderer.api.data.Uniform.FloatArrayUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.model.Model;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.engine.render.FlowRenderer;

public class GaussianBlurStage extends Creatable {
    private final FlowRenderer renderer;
    private final Material horizontalMaterial;
    private final Material verticalMaterial;
    private final FrameBuffer horizontalFrameBuffer;
    private final FrameBuffer verticalFrameBuffer;
    private final Texture intermediateTexture;
    private final Texture colorsOutput;
    private Texture colorsInput;
    private Pipeline pipeline;
    private int kernelSize = 5;

    public GaussianBlurStage(FlowRenderer renderer) {
        this.renderer = renderer;
        final Program blurProgram = renderer.getProgram("gaussianBlur");
        horizontalMaterial = new Material(blurProgram);
        verticalMaterial = new Material(blurProgram);
        final GLFactory glFactory = renderer.getGLFactory();
        horizontalFrameBuffer = glFactory.createFrameBuffer();
        verticalFrameBuffer = glFactory.createFrameBuffer();
        intermediateTexture = glFactory.createTexture();
        colorsOutput = glFactory.createTexture();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Guassian blur stage has already been created");
        }
        // Generate the kernel and offsets
        int halfKernelSize = (kernelSize - 1) / 2 + 1;
        float[] kernel = new float[halfKernelSize];
        float[] offsets = new float[halfKernelSize];
        float weight0 = getGaussianWeight(0, kernelSize);
        kernel[0] = weight0;
        offsets[0] = 0;
        float sum = weight0;
        for (int i = 1; i < kernelSize; i += 2) {
            final float firstWeight = getGaussianWeight(i, kernelSize);
            final float secondWeight = getGaussianWeight(i + 1, kernelSize);
            final float weightSum = firstWeight + secondWeight;
            sum += weightSum * 2;
            final int index = (i + 1) / 2;
            kernel[index] = weightSum;
            offsets[index] = (i * firstWeight + (i + 1) * secondWeight) / weightSum;
        }
        for (int i = 0; i < halfKernelSize; i++) {
            kernel[i] /= sum;
        }
        // Create the colors texture
        colorsOutput.setFormat(Format.RGBA);
        colorsOutput.setInternalFormat(InternalFormat.RGBA8);
        colorsOutput.setImageData(null, FlowRenderer.WINDOW_SIZE.getFloorX(), FlowRenderer.WINDOW_SIZE.getFloorY());
        colorsOutput.setWrapS(WrapMode.CLAMP_TO_EDGE);
        colorsOutput.setWrapT(WrapMode.CLAMP_TO_EDGE);
        colorsOutput.setMagFilter(FilterMode.LINEAR);
        colorsOutput.setMinFilter(FilterMode.LINEAR);
        colorsOutput.create();
        // Create the intermediate texture
        intermediateTexture.setFormat(Format.RGBA);
        intermediateTexture.setInternalFormat(InternalFormat.RGBA8);
        intermediateTexture.setImageData(null, colorsOutput.getWidth(), colorsOutput.getHeight());
        intermediateTexture.setWrapS(WrapMode.CLAMP_TO_EDGE);
        intermediateTexture.setWrapT(WrapMode.CLAMP_TO_EDGE);
        intermediateTexture.setMagFilter(FilterMode.LINEAR);
        intermediateTexture.setMinFilter(FilterMode.LINEAR);
        intermediateTexture.create();
        // Create the shared uniforms
        final FloatArrayUniform offsetsUniform = new FloatArrayUniform("offsets", offsets);
        final IntUniform kernelSizeUniform = new IntUniform("kernelSize", halfKernelSize);
        final FloatArrayUniform kernelUniform = new FloatArrayUniform("kernel", kernel);
        final Vector2Uniform resolutionUniform = new Vector2Uniform("resolution", new Vector2f(colorsOutput.getWidth(), colorsOutput.getHeight()));
        // Create the horizontal material
        horizontalMaterial.addTexture(0, colorsInput);
        UniformHolder uniforms = horizontalMaterial.getUniforms();
        uniforms.add(offsetsUniform);
        uniforms.add(kernelSizeUniform);
        uniforms.add(kernelUniform);
        uniforms.add(resolutionUniform);
        uniforms.add(new BooleanUniform("direction", false));
        // Create the vertical material
        verticalMaterial.addTexture(0, intermediateTexture);
        uniforms = verticalMaterial.getUniforms();
        uniforms.add(offsetsUniform);
        uniforms.add(kernelSizeUniform);
        uniforms.add(kernelUniform);
        uniforms.add(resolutionUniform);
        uniforms.add(new BooleanUniform("direction", true));
        // Create the horizontal screen model
        final Model horizontalModel = new Model(renderer.getScreen(), horizontalMaterial);
        // Create the vertical screen model
        final Model verticalModel = new Model(renderer.getScreen(), verticalMaterial);
        // Create the frame buffer
        horizontalFrameBuffer.attach(AttachmentPoint.COLOR0, intermediateTexture);
        horizontalFrameBuffer.create();
        // Create the vertical frame buffer
        verticalFrameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
        verticalFrameBuffer.create();
        // Create the pipeline
        pipeline = new PipelineBuilder().bindFrameBuffer(horizontalFrameBuffer).renderModels(Arrays.asList(horizontalModel)).bindFrameBuffer(verticalFrameBuffer)
                .renderModels(Arrays.asList(verticalModel)).unbindFrameBuffer(verticalFrameBuffer).build();
        // Update state to created
        super.create();
    }

    @Override
    public void destroy() {
        checkCreated();
        horizontalFrameBuffer.destroy();
        verticalFrameBuffer.destroy();
        intermediateTexture.destroy();
        colorsOutput.destroy();
        super.destroy();
    }

    public void render() {
        checkCreated();
        pipeline.run(renderer.getContext());
    }

    public void setKernelSize(int kernelSize) {
        if ((kernelSize & 1) == 0) {
            kernelSize--;
        }
        if (kernelSize <= 1) {
            throw new IllegalArgumentException("Kernel size must be at least 3");
        }
        this.kernelSize = kernelSize;
    }

    public void setColorsInput(Texture texture) {
        texture.checkCreated();
        colorsInput = texture;
    }

    public Texture getColorsOutput() {
        return colorsOutput;
    }

    private static float getGaussianWeight(float x, float radius) {
        x /= radius;
        return (float) Math.exp(-(x * x));
    }
}