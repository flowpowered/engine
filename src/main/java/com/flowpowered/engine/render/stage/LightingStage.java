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

import com.flowpowered.engine.render.FlowRenderer;
import org.spout.renderer.api.Creatable;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.model.Model;

/**
 *
 */
public class LightingStage extends Creatable {
    private final FlowRenderer renderer;
    private final Material material;
    private final FrameBuffer frameBuffer;
    private final Texture colorsOutput;
    private Texture colorsInput;
    private Texture normalsInput;
    private Texture depthsInput;
    private Texture materialInput;
    private Texture occlusionsInput;
    private Texture shadowsInput;
    private Pipeline pipeline;

    public LightingStage(FlowRenderer renderer) {
        this.renderer = renderer;
        material = new Material(renderer.getProgram("lighting"));
        final GLFactory glFactory = renderer.getGLFactory();
        frameBuffer = glFactory.createFrameBuffer();
        colorsOutput = glFactory.createTexture();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Lighting stage has already been created");
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
        // Create the material
        material.addTexture(0, colorsInput);
        material.addTexture(1, normalsInput);
        material.addTexture(2, depthsInput);
        material.addTexture(3, materialInput);
        material.addTexture(4, occlusionsInput);
        material.addTexture(5, shadowsInput);
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(new FloatUniform("tanHalfFOV", FlowRenderer.TAN_HALF_FOV));
        uniforms.add(new FloatUniform("aspectRatio", FlowRenderer.ASPECT_RATIO));
        uniforms.add(renderer.getLightDirectionUniform());
        // Create the screen model
        final Model model = new Model(renderer.getScreen(), material);
        // Create the frame buffer
        frameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
        frameBuffer.create();
        // Create the pipeline
        pipeline = new PipelineBuilder().bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
        // Update state to created
        super.create();
    }

    @Override
    public void destroy() {
        checkCreated();
        colorsOutput.destroy();
        super.destroy();
    }

    public void render() {
        checkCreated();
        pipeline.run(renderer.getContext());
    }

    public void setColorsInput(Texture texture) {
        texture.checkCreated();
        colorsInput = texture;
    }

    public void setNormalsInput(Texture texture) {
        texture.checkCreated();
        normalsInput = texture;
    }

    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        depthsInput = texture;
    }

    public void setMaterialInput(Texture texture) {
        texture.checkCreated();
        materialInput = texture;
    }

    public void setOcclusionsInput(Texture texture) {
        texture.checkCreated();
        occlusionsInput = texture;
    }

    public void setShadowsInput(Texture texture) {
        texture.checkCreated();
        shadowsInput = texture;
    }

    public Texture getColorsOutput() {
        return colorsOutput;
    }
}
