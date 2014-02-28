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

import java.util.Arrays;

import com.flowpowered.math.vector.Vector2f;

import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.BooleanUniform;
import org.spout.renderer.api.data.Uniform.FloatArrayUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.model.Model;

import com.flowpowered.engine.render.graph.RenderGraph;

/**
 *
 */
public class BlurNode extends GraphNode {
    public static final KernelGenerator GAUSSIAN_KERNEL = new KernelGenerator() {
        @Override
        public float getWeight(float x, float radius) {
            x /= radius;
            return (float) Math.exp(-(x * x));
        }
    };
    public static final KernelGenerator BOX_KERNEL = new KernelGenerator() {
        @Override
        public float getWeight(float x, float radius) {
            return 1;
        }
    };
    private final Material horizontalMaterial;
    private final Material verticalMaterial;
    private final FrameBuffer horizontalFrameBuffer;
    private final FrameBuffer verticalFrameBuffer;
    private final Texture intermediateTexture;
    private final Texture colorsOutput;
    private Texture colorsInput;
    private Pipeline pipeline;
    private final IntUniform halfKernelSizeUniform = new IntUniform("kernelSize", 0);
    private final FloatArrayUniform kernelUniform = new FloatArrayUniform("kernel", new float[]{});
    private final FloatArrayUniform offsetsUniform = new FloatArrayUniform("offsets", new float[]{});
    private final Vector2Uniform resolutionUniform = new Vector2Uniform("resolution", Vector2f.ONE);
    private KernelGenerator kernelGenerator = GAUSSIAN_KERNEL;

    public BlurNode(RenderGraph graph, String name) {
        super(graph, name);
        final Program blurProgram = graph.getProgram("blur");
        horizontalMaterial = new Material(blurProgram);
        verticalMaterial = new Material(blurProgram);
        final Context context = graph.getContext();
        horizontalFrameBuffer = context.newFrameBuffer();
        verticalFrameBuffer = context.newFrameBuffer();
        intermediateTexture = context.newTexture();
        colorsOutput = context.newTexture();
    }

    @Override
    public void create() {
        checkNotCreated();
        final Format format = colorsInput.getFormat();
        final InternalFormat internalFormat = colorsInput.getInternalFormat();
        // Create the colors texture
        colorsOutput.create();
        colorsOutput.setFormat(format, internalFormat);
        colorsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        colorsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        colorsOutput.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the intermediate texture
        intermediateTexture.create();
        intermediateTexture.setFormat(format, internalFormat);
        intermediateTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        intermediateTexture.setImageData(null, colorsInput.getWidth(), colorsInput.getHeight());
        intermediateTexture.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the horizontal material
        horizontalMaterial.addTexture(0, colorsInput);
        UniformHolder uniforms = horizontalMaterial.getUniforms();
        uniforms.add(offsetsUniform);
        uniforms.add(halfKernelSizeUniform);
        uniforms.add(kernelUniform);
        uniforms.add(resolutionUniform);
        uniforms.add(new BooleanUniform("direction", false));
        // Create the vertical material
        verticalMaterial.addTexture(0, intermediateTexture);
        uniforms = verticalMaterial.getUniforms();
        uniforms.add(offsetsUniform);
        uniforms.add(halfKernelSizeUniform);
        uniforms.add(kernelUniform);
        uniforms.add(resolutionUniform);
        uniforms.add(new BooleanUniform("direction", true));
        // Create the horizontal screen model
        final Model horizontalModel = new Model(graph.getScreen(), horizontalMaterial);
        // Create the vertical screen model
        final Model verticalModel = new Model(graph.getScreen(), verticalMaterial);
        // Create the frame buffer
        horizontalFrameBuffer.create();
        horizontalFrameBuffer.attach(AttachmentPoint.COLOR0, intermediateTexture);
        // Create the vertical frame buffer
        verticalFrameBuffer.create();
        verticalFrameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
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

    @Override
    public void render() {
        checkCreated();
        pipeline.run(graph.getContext());
    }

    @Setting
    public void setKernelSize(int kernelSize) {
        if ((kernelSize & 1) == 0) {
            kernelSize--;
        }
        if (kernelSize <= 1) {
            throw new IllegalArgumentException("Kernel size must be at least 3");
        }
        // Generate the kernel and offsets
        final int halfKernelSize = (kernelSize - 1) / 2 + 1;
        final float[] kernel = new float[halfKernelSize];
        final float[] offsets = new float[halfKernelSize];
        float weight0 = kernelGenerator.getWeight(0, kernelSize);
        kernel[0] = weight0;
        offsets[0] = 0;
        float sum = weight0;
        for (int i = 1; i < kernelSize; i += 2) {
            final float firstWeight = kernelGenerator.getWeight(i, kernelSize);
            final float secondWeight = kernelGenerator.getWeight(i + 1, kernelSize);
            final float weightSum = firstWeight + secondWeight;
            sum += weightSum * 2;
            final int index = (i + 1) / 2;
            kernel[index] = weightSum;
            offsets[index] = (i * firstWeight + (i + 1) * secondWeight) / weightSum;
        }
        for (int i = 0; i < halfKernelSize; i++) {
            kernel[i] /= sum;
        }
        // Update the uniforms
        halfKernelSizeUniform.set(halfKernelSize);
        kernelUniform.set(kernel);
        offsetsUniform.set(offsets);
    }

    @Setting
    public void setKernelGenerator(KernelGenerator kernelGenerator) {
        this.kernelGenerator = kernelGenerator;
    }

    @Input("colors")
    public void setColorsInput(Texture texture) {
        texture.checkCreated();
        colorsInput = texture;
        resolutionUniform.set(new Vector2f(texture.getWidth(), texture.getHeight()));
    }

    @Output("colors")
    public Texture getColorsOutput() {
        return colorsOutput;
    }

    public static interface KernelGenerator {
        public float getWeight(float x, float radius);
    }
}