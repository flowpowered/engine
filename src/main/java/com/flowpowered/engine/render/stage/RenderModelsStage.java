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

import java.util.ArrayList;
import java.util.List;

import com.flowpowered.engine.render.FlowRenderer;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Creatable;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
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
public class RenderModelsStage extends Creatable {
    private final FlowRenderer renderer;
    private final FrameBuffer frameBuffer;
    private final Texture colorsOutput;
    private final Texture normalsOutput;
    private final Texture depthsOutput;
    private final Texture vertexNormalsOutput;
    private final Texture materialsOutput;
    private final List<Model> models = new ArrayList<>();
    private final Camera camera = Camera.createPerspective(FlowRenderer.FIELD_OF_VIEW, FlowRenderer.WINDOW_SIZE.getFloorX(), FlowRenderer.WINDOW_SIZE.getFloorY(), FlowRenderer.NEAR_PLANE, FlowRenderer.FAR_PLANE);
    private Pipeline pipeline;

    public RenderModelsStage(FlowRenderer renderer) {
        this.renderer = renderer;
        final GLFactory glFactory = renderer.getGLFactory();
        colorsOutput = glFactory.createTexture();
        normalsOutput = glFactory.createTexture();
        depthsOutput = glFactory.createTexture();
        vertexNormalsOutput = glFactory.createTexture();
        materialsOutput = glFactory.createTexture();
        frameBuffer = glFactory.createFrameBuffer();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Render models stage has already been created");
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
        // Create the normals texture
        normalsOutput.setFormat(Format.RGBA);
        normalsOutput.setInternalFormat(InternalFormat.RGBA8);
        normalsOutput.setImageData(null, FlowRenderer.WINDOW_SIZE.getFloorX(), FlowRenderer.WINDOW_SIZE.getFloorY());
        normalsOutput.create();
        // Create the detphs texture
        depthsOutput.setFormat(Format.DEPTH);
        depthsOutput.setInternalFormat(InternalFormat.DEPTH_COMPONENT32);
        depthsOutput.setImageData(null, FlowRenderer.WINDOW_SIZE.getFloorX(), FlowRenderer.WINDOW_SIZE.getFloorY());
        depthsOutput.setWrapS(WrapMode.CLAMP_TO_EDGE);
        depthsOutput.setWrapT(WrapMode.CLAMP_TO_EDGE);
        depthsOutput.create();
        // Create the vertex normals texture
        vertexNormalsOutput.setFormat(Format.RGBA);
        vertexNormalsOutput.setInternalFormat(InternalFormat.RGBA8);
        vertexNormalsOutput.setImageData(null, FlowRenderer.WINDOW_SIZE.getFloorX(), FlowRenderer.WINDOW_SIZE.getFloorY());
        vertexNormalsOutput.create();
        // Create the materials texture
        materialsOutput.setFormat(Format.RGBA);
        materialsOutput.setInternalFormat(InternalFormat.RGBA8);
        materialsOutput.setImageData(null, FlowRenderer.WINDOW_SIZE.getFloorX(), FlowRenderer.WINDOW_SIZE.getFloorY());
        materialsOutput.create();
        // Create the frame buffer
        frameBuffer.attach(AttachmentPoint.COLOR0, colorsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR1, normalsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR2, vertexNormalsOutput);
        frameBuffer.attach(AttachmentPoint.COLOR3, materialsOutput);
        frameBuffer.attach(AttachmentPoint.DEPTH, depthsOutput);
        frameBuffer.create();
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

    public void render() {
        checkCreated();
        pipeline.run(renderer.getContext());
    }

    public Texture getColorsOutput() {
        return colorsOutput;
    }

    public Texture getNormalsOutput() {
        return normalsOutput;
    }

    public Texture getDepthsOutput() {
        return depthsOutput;
    }

    public Texture getVertexNormalsOutput() {
        return vertexNormalsOutput;
    }

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
