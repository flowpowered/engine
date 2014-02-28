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
import java.util.List;

import com.flowpowered.commons.ViewFrustum;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.CompareMode;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.Rectangle;

import com.flowpowered.engine.render.graph.RenderGraph;

public class CascadedShadowMappingNode extends ShadowMappingNode {
    private final Texture lightDepthsTexture2;
    private final Texture lightDepthsTexture3;
    private final FrameBuffer depthFrameBuffer2;
    private final FrameBuffer depthFrameBuffer3;
    private final Matrix4Uniform lightViewMatrixUniform2 = new Matrix4Uniform("lightViewMatrix2", new Matrix4f());
    private final Matrix4Uniform lightViewMatrixUniform3 = new Matrix4Uniform("lightViewMatrix3", new Matrix4f());
    private final Matrix4Uniform lightProjectionMatrixUniform2 = new Matrix4Uniform("lightProjectionMatrix2", new Matrix4f());
    private final Matrix4Uniform lightProjectionMatrixUniform3 = new Matrix4Uniform("lightProjectionMatrix3", new Matrix4f());
    private final Vector2Uniform slicesUniform = new Vector2Uniform("slices", Vector2f.ZERO);
    private final Camera camera2 = Camera.createOrthographic(50, -50, 50, -50, -50, 50);
    private final Camera camera3 = Camera.createOrthographic(50, -50, 50, -50, -50, 50);

    public CascadedShadowMappingNode(RenderGraph graph, String name) {
        super(graph, name, "cascadedShadow");
        final Context context = graph.getContext();
        lightDepthsTexture2 = context.newTexture();
        lightDepthsTexture3 = context.newTexture();
        depthFrameBuffer2 = context.newFrameBuffer();
        depthFrameBuffer3 = context.newFrameBuffer();
    }

    @Override
    public void create() {
        checkNotCreated();
        // Let the super class create the basis
        super.create();
        // Create second light depth texture
        lightDepthsTexture2.create();
        lightDepthsTexture2.setFormat(Format.DEPTH, InternalFormat.DEPTH_COMPONENT16);
        lightDepthsTexture2.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        lightDepthsTexture2.setImageData(null, shadowMapSize.getX(), shadowMapSize.getY());
        lightDepthsTexture2.setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture2.setCompareMode(CompareMode.LESS);
        // Create third light depth texture
        lightDepthsTexture3.create();
        lightDepthsTexture3.setFormat(Format.DEPTH, InternalFormat.DEPTH_COMPONENT16);
        lightDepthsTexture3.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        lightDepthsTexture3.setImageData(null, shadowMapSize.getX(), shadowMapSize.getY());
        lightDepthsTexture3.setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture3.setCompareMode(CompareMode.LESS);
        // Update the material
        material.addTexture(3, lightDepthsTexture2);
        material.addTexture(4, lightDepthsTexture3);
        material.addTexture(5, noiseTexture);
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(lightViewMatrixUniform2);
        uniforms.add(lightViewMatrixUniform3);
        uniforms.add(lightProjectionMatrixUniform2);
        uniforms.add(lightProjectionMatrixUniform3);
        uniforms.add(slicesUniform);
        // Create the second depth frame buffer
        depthFrameBuffer2.create();
        depthFrameBuffer2.attach(AttachmentPoint.DEPTH, lightDepthsTexture2);
        // Create the third depth frame buffer
        depthFrameBuffer3.create();
        depthFrameBuffer3.attach(AttachmentPoint.DEPTH, lightDepthsTexture3);
    }

    @Override
    protected Pipeline createPipeline(Model model) {
        final RenderModelsNode renderModelsNode = (RenderModelsNode) graph.getNode("models");
        final List<Model> models = renderModelsNode.getModels();
        return new PipelineBuilder().useViewPort(new Rectangle(Vector2i.ZERO, shadowMapSize))
                .useCamera(camera).bindFrameBuffer(depthFrameBuffer).clearBuffer().doAction(new RenderShadowModelsAction(models))
                .useCamera(camera2).bindFrameBuffer(depthFrameBuffer2).clearBuffer().doAction(new RenderShadowModelsAction(models))
                .useCamera(camera3).bindFrameBuffer(depthFrameBuffer3).clearBuffer().doAction(new RenderShadowModelsAction(models))
                .useViewPort(new Rectangle(Vector2i.ZERO, graph.getWindowSize())).useCamera(renderModelsNode.getCamera())
                .bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
    }

    @Override
    public void destroy() {
        checkCreated();
        lightDepthsTexture2.destroy();
        lightDepthsTexture3.destroy();
        depthFrameBuffer2.destroy();
        depthFrameBuffer3.destroy();
        // Let the super class destroy the rest
        super.destroy();
    }

    @Override
    public void render() {
        lightViewMatrixUniform2.set(camera2.getViewMatrix());
        lightViewMatrixUniform3.set(camera3.getViewMatrix());
        lightProjectionMatrixUniform2.set(camera2.getProjectionMatrix());
        lightProjectionMatrixUniform3.set(camera3.getProjectionMatrix());
        super.render();
    }

    @Override
    public void updateLight(Vector3f direction, ViewFrustum frustum) {
        // Set the direction uniform
        direction = direction.normalize();
        setLightDirection(direction);
        // Calculate the camera rotation from the direction and set
        final Quaternionf rotation = Quaternionf.fromRotationTo(Vector3f.FORWARD.negate(), direction);
        // Calculate the transformation from the camera bounds rotation to the identity rotation (its axis aligned space)
        final Matrix3f axisAlignTransform = Matrix3f.createRotation(rotation).invert();
        // Calculate the points of the box to completely include inside the camera bounds
        // Transform those points to the axis aligned space of the camera bounds
        final Vector3f position = frustum.getPosition();
        final Vector3f[] vertices = frustum.getVertices();
        for (int i = 0; i < 8; i++) {
            vertices[i] = axisAlignTransform.transform(vertices[i].sub(position));
        }
        // Clone the vertices for first and third slices
        final Vector3f[] slice1Vertices = vertices.clone();
        final Vector3f[] slice3Vertices = vertices.clone();
        // Compute the slices
        final float near = graph.getNearPlane();
        final float far = graph.getFarPlane();
        float slice1 = computeSlice(1, 3, 0.7f, near, far);
        float slice2 = computeSlice(2, 3, 0.7f, near, far);
        // Update the slices uniform
        slicesUniform.set(new Vector2f(slice1, slice2));
        // Normalize the slices
        slice1 /= far;
        slice2 /= far;
        // Rescale the vertices for the first slice
        slice1Vertices[1] = slice1Vertices[1].sub(slice1Vertices[0]).mul(slice1).add(slice1Vertices[0]);
        slice1Vertices[3] = slice1Vertices[3].sub(slice1Vertices[2]).mul(slice1).add(slice1Vertices[2]);
        slice1Vertices[5] = slice1Vertices[5].sub(slice1Vertices[4]).mul(slice1).add(slice1Vertices[4]);
        slice1Vertices[7] = slice1Vertices[7].sub(slice1Vertices[6]).mul(slice1).add(slice1Vertices[6]);
        // Calculate the new camera bounds so that the box is fully included in those bounds
        fitFrustum(camera, position, rotation, slice1Vertices);
        // Rescale the vertices for the third slice
        slice3Vertices[0] = slice3Vertices[0].sub(slice3Vertices[1]).mul(1 - slice2).add(slice3Vertices[1]);
        slice3Vertices[2] = slice3Vertices[2].sub(slice3Vertices[3]).mul(1 - slice2).add(slice3Vertices[3]);
        slice3Vertices[4] = slice3Vertices[4].sub(slice3Vertices[5]).mul(1 - slice2).add(slice3Vertices[5]);
        slice3Vertices[6] = slice3Vertices[6].sub(slice3Vertices[7]).mul(1 - slice2).add(slice3Vertices[7]);
        // Calculate the new camera bounds so that the box is fully included in those bounds
        fitFrustum(camera3, position, rotation, slice3Vertices);
        // The vertices for the second slice are a combination of slice 1 and 3, between the two
        vertices[0] = slice1Vertices[1];
        vertices[1] = slice3Vertices[0];
        vertices[2] = slice1Vertices[3];
        vertices[3] = slice3Vertices[2];
        vertices[4] = slice1Vertices[5];
        vertices[5] = slice3Vertices[4];
        vertices[6] = slice1Vertices[7];
        vertices[7] = slice3Vertices[6];
        // Calculate the new camera bounds so that the box is fully included in those bounds
        fitFrustum(camera2, position, rotation, vertices);
    }

    private static void fitFrustum(Camera camera, Vector3f position, Quaternionf rotation, Vector3f[] frustum) {
        // Calculate the new camera bounds so that the box is fully included in those bounds
        final Vector3f low = frustum[0].min(frustum[1]).min(frustum[2]).min(frustum[3])
                .min(frustum[4]).min(frustum[5]).min(frustum[6]).min(frustum[7]);
        final Vector3f high = frustum[0].max(frustum[1]).max(frustum[2]).max(frustum[3])
                .max(frustum[4]).max(frustum[5]).max(frustum[6]).max(frustum[7]);
        // Calculate the size of the new camera bounds
        final Vector3f size = high.sub(low).div(2);
        final Vector3f mid = low.add(size);
        // Compute the camera position
        position = Matrix3f.createRotation(rotation).transform(mid).add(position);
        // Update the camera position
        camera.setPosition(position);
        // Update the camera rotation
        camera.setRotation(rotation);
        // Update the camera size
        camera.setProjection(Matrix4f.createOrthographic(size.getX(), -size.getX(), size.getY(), -size.getY(), -size.getZ(), size.getZ()));
    }

    private static float computeSlice(int index, int total, float correction, float near, float far) {
        return (float) (correction * near * Math.pow(far / near, index / (float) total) + (1 - correction) * (near + (index / (float) total) * (far - near)));
    }
}