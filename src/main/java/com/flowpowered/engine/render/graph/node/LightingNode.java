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

import com.flowpowered.engine.render.graph.RenderGraph;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.Rectangle;

public class LightingNode extends GraphNode {
    private final FrameBuffer frameBuffer;
    private final Texture colorsOutput;
    private final Material material;
    private final Pipeline pipeline;
    private final Rectangle outputSize = new Rectangle();
    private final FloatUniform aspectRatioUniform = new FloatUniform("aspectRatio", 1);
    private final FloatUniform tanHalfFOVUniform = new FloatUniform("tanHalfFOV", 1);
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", Vector3f.UP.negate());

    public LightingNode(RenderGraph graph, String name) {
        super(graph, name);
        final Context context = graph.getContext();
        // Create the colors texture
        colorsOutput = context.newTexture();
        colorsOutput.create();
        colorsOutput.setFormat(InternalFormat.RGBA8);
        colorsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        colorsOutput.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the frame buffer
        frameBuffer = context.newFrameBuffer();
        frameBuffer.create();
        frameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
        // Create the material
        material = new Material(graph.getProgram("lighting"));
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(aspectRatioUniform);
        uniforms.add(tanHalfFOVUniform);
        uniforms.add(lightDirectionUniform);
        // Create the screen model
        final Model model = new Model(graph.getScreen(), material);
        // Create the pipeline
        pipeline = new PipelineBuilder().useViewPort(outputSize).bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
    }

    @Override
    public void destroy() {
        frameBuffer.destroy();
        colorsOutput.destroy();
    }

    @Override
    public void render() {
        pipeline.run(graph.getContext());
    }

    @Setting
    public void setLightDirection(Vector3f lightDirection) {
        lightDirectionUniform.set(lightDirection);
    }

    @Setting
    public void setFieldOfView(float fieldOfView) {
        tanHalfFOVUniform.set(TrigMath.tan(Math.toRadians(fieldOfView) / 2));
    }

    @Input("colors")
    public void setColorsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(0, texture);
    }

    @Input("normals")
    public void setNormalsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(1, texture);
    }

    @Input("depths")
    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(2, texture);
        aspectRatioUniform.set((float) texture.getWidth() / texture.getHeight());
    }

    @Input("materials")
    public void setMaterialsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(3, texture);
    }

    @Input("occlusions")
    public void setOcclusionsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(4, texture);
    }

    @Input("shadows")
    public void setShadowsInput(Texture texture) {
        texture.checkCreated();
        material.addTexture(5, texture);
    }

    @Output("colors")
    public Texture getColorsOutput() {
        return colorsOutput;
    }

    @Setting
    public void setColorsSize(Vector2i size) {
        outputSize.setSize(size);
        colorsOutput.setImageData(null, size.getX(), size.getY());
    }
}