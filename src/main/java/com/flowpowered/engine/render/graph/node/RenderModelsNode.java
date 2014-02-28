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

import java.util.ArrayList;
import java.util.List;

import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
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
public class RenderModelsNode extends GraphNode {
    private final FrameBuffer frameBuffer;
    private final Texture colorsOutput;
    private final Texture normalsOutput;
    private final Texture depthsOutput;
    private final Texture vertexNormalsOutput;
    private final Texture materialsOutput;
    private final List<Model> models = new ArrayList<>();
    private final Camera camera;
    private Pipeline pipeline;

    public RenderModelsNode(RenderGraph graph, String name) {
        super(graph, name);
        final Context context = graph.getContext();
        colorsOutput = context.newTexture();
        normalsOutput = context.newTexture();
        depthsOutput = context.newTexture();
        vertexNormalsOutput = context.newTexture();
        materialsOutput = context.newTexture();
        frameBuffer = context.newFrameBuffer();
        camera = Camera.createPerspective(graph.getFieldOfView(), graph.getWindowWidth(), graph.getWindowHeight(), graph.getNearPlane(), graph.getFarPlane());
    }

    @Override
    public void create() {
        checkNotCreated();
        // Create the colors texture
        colorsOutput.create();
        colorsOutput.setFormat(Format.RGBA, InternalFormat.RGBA8);
        colorsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        colorsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        colorsOutput.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the normals texture
        normalsOutput.create();
        normalsOutput.setFormat(Format.RGBA, InternalFormat.RGBA8);
        normalsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        normalsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        // Create the depths texture
        depthsOutput.create();
        depthsOutput.setFormat(Format.DEPTH, InternalFormat.DEPTH_COMPONENT32);
        depthsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        depthsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        depthsOutput.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // Create the vertex normals texture
        vertexNormalsOutput.create();
        vertexNormalsOutput.setFormat(Format.RGBA, InternalFormat.RGBA8);
        vertexNormalsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        vertexNormalsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        // Create the materials texture
        materialsOutput.create();
        materialsOutput.setFormat(Format.RGBA, InternalFormat.RGBA8);
        materialsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        materialsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        // Create the frame buffer
        frameBuffer.create();
        frameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR1, normalsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR2, vertexNormalsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR3, materialsOutput);
        frameBuffer.attach(AttachmentPoint.DEPTH, depthsOutput);
        // Create the pipeline
        pipeline = new PipelineBuilder().useCamera(camera).bindFrameBuffer(frameBuffer).clearBuffer().renderModels(models).unbindFrameBuffer(frameBuffer).build();
        // Update the state to created
        super.create();
    }

    @Override
    public void destroy() {
        checkCreated();
        frameBuffer.destroy();
        colorsOutput.destroy();
        normalsOutput.destroy();
        depthsOutput.destroy();
        vertexNormalsOutput.destroy();
        materialsOutput.destroy();
        super.destroy();
    }

    @Override
    public void render() {
        checkCreated();
        pipeline.run(graph.getContext());
    }

    @Output("colors")
    public Texture getColorsOutput() {
        return colorsOutput;
    }

    @Output("normals")
    public Texture getNormalsOutput() {
        return normalsOutput;
    }

    @Output("depths")
    public Texture getDepthsOutput() {
        return depthsOutput;
    }

    @Output("vertexNormals")
    public Texture getVertexNormalsOutput() {
        return vertexNormalsOutput;
    }

    @Output("materials")
    public Texture getMaterialsOutput() {
        return materialsOutput;
    }

    public Camera getCamera() {
        return camera;
    }

    /**
     * Adds a model to the renderer.
     *
     * @param model The model to add
     */
    public void addModel(Model model) {
        model.getUniforms().add(new Matrix4Uniform("previousModelMatrix", model.getMatrix()));
        models.add(model);
    }

    /**
     * Removes a model from the renderer.
     *
     * @param model The model to remove
     */
    public void removeModel(Model model) {
        models.remove(model);
    }

    /**
     * Removes all the models from the renderer.
     */
    public void clearModels() {
        models.clear();
    }

    public List<Model> getModels() {
        return models;
    }
}