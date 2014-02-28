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
import java.util.Collection;
import java.util.Random;

import com.flowpowered.commons.ViewFrustum;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import org.spout.renderer.api.Action;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector2ArrayUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.CompareMode;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.CausticUtil;
import org.spout.renderer.api.util.Rectangle;

import com.flowpowered.engine.render.graph.RenderGraph;

public class ShadowMappingNode extends GraphNode {
    protected final Material material;
    private final Texture lightDepthsTexture;
    protected final Texture noiseTexture;
    protected final FrameBuffer depthFrameBuffer;
    protected final FrameBuffer frameBuffer;
    private final Texture shadowsOutput;
    private Texture normalsInput;
    private Texture depthsInput;
    private final Matrix4Uniform inverseViewMatrixUniform = new Matrix4Uniform("inverseViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightViewMatrixUniform = new Matrix4Uniform("lightViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightProjectionMatrixUniform = new Matrix4Uniform("lightProjectionMatrix", new Matrix4f());
    protected final Camera camera = Camera.createOrthographic(50, -50, 50, -50, -50, 50);
    protected Vector2i shadowMapSize = new Vector2i(1024, 1024);
    private Pipeline pipeline;
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", Vector3f.UP.negate());
    private final IntUniform kernelSizeUniform = new IntUniform("kernelSize", 0);
    private final Vector2ArrayUniform kernelUniform = new Vector2ArrayUniform("kernel", new Vector2f[]{});
    private final Vector2Uniform noiseScaleUniform = new Vector2Uniform("noiseScale", Vector2f.ONE);
    private final FloatUniform biasUniform = new FloatUniform("bias", 0.005f);
    private final FloatUniform radiusUniform = new FloatUniform("radius", 0.0004f);

    public ShadowMappingNode(RenderGraph graph, String name) {
        this(graph, name, "shadow");
    }

    protected ShadowMappingNode(RenderGraph graph, String name, String program) {
        super(graph, name);
        material = new Material(graph.getProgram(program));
        final Context context = graph.getContext();
        lightDepthsTexture = context.newTexture();
        noiseTexture = context.newTexture();
        shadowsOutput = context.newTexture();
        depthFrameBuffer = context.newFrameBuffer();
        frameBuffer = context.newFrameBuffer();
    }

    @Override
    public void create() {
        checkNotCreated();
        // Create the noise texture
        noiseTexture.create();
        noiseTexture.setFormat(Format.RG, InternalFormat.RG8);
        noiseTexture.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        // Create the shadows texture
        shadowsOutput.create();
        shadowsOutput.setFormat(Format.RED, InternalFormat.R8);
        shadowsOutput.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        shadowsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        // Create the depth texture
        lightDepthsTexture.create();
        lightDepthsTexture.setFormat(Format.DEPTH, InternalFormat.DEPTH_COMPONENT16);
        lightDepthsTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        lightDepthsTexture.setImageData(null, shadowMapSize.getX(), shadowMapSize.getY());
        lightDepthsTexture.setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture.setCompareMode(CompareMode.LESS);
        // Create the material
        material.addTexture(0, normalsInput);
        material.addTexture(1, depthsInput);
        material.addTexture(2, lightDepthsTexture);
        material.addTexture(3, noiseTexture);
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(graph.getProjectionUniform());
        uniforms.add(graph.getTanHalfFOVUniform());
        uniforms.add(graph.getAspectRatioUniform());
        uniforms.add(lightDirectionUniform);
        uniforms.add(inverseViewMatrixUniform);
        uniforms.add(lightViewMatrixUniform);
        uniforms.add(lightProjectionMatrixUniform);
        uniforms.add(kernelSizeUniform);
        uniforms.add(kernelUniform);
        uniforms.add(noiseScaleUniform);
        uniforms.add(biasUniform);
        uniforms.add(radiusUniform);
        // Create the screen model
        final Model model = new Model(graph.getScreen(), material);
        // Create the depth frame buffer
        depthFrameBuffer.create();
        depthFrameBuffer.attach(AttachmentPoint.DEPTH, lightDepthsTexture);
        // Create the frame buffer
        frameBuffer.create();
        frameBuffer.attach(AttachmentPoint.COLOR0, shadowsOutput);
        // Create the pipeline
        pipeline = createPipeline(model);
        // Update state to created
        super.create();
    }

    protected Pipeline createPipeline(Model model) {
        final RenderModelsNode renderModelsNode = (RenderModelsNode) graph.getNode("models");
        return new PipelineBuilder().useViewPort(new Rectangle(Vector2i.ZERO, shadowMapSize))
                .useCamera(camera).bindFrameBuffer(depthFrameBuffer).clearBuffer().doAction(new RenderShadowModelsAction(renderModelsNode.getModels()))
                .useViewPort(new Rectangle(Vector2i.ZERO, graph.getWindowSize())).useCamera(renderModelsNode.getCamera())
                .bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
    }

    @Override
    public void destroy() {
        checkCreated();
        lightDepthsTexture.destroy();
        noiseTexture.destroy();
        depthFrameBuffer.destroy();
        frameBuffer.destroy();
        shadowsOutput.destroy();
        super.destroy();
    }

    @Override
    public void render() {
        inverseViewMatrixUniform.set(((RenderModelsNode) graph.getNode("models")).getCamera().getViewMatrix().invert());
        lightViewMatrixUniform.set(camera.getViewMatrix());
        lightProjectionMatrixUniform.set(camera.getProjectionMatrix());
        pipeline.run(graph.getContext());
    }

    @Setting
    public void setShadowMapSize(Vector2i shadowMapSize) {
        this.shadowMapSize = shadowMapSize;
    }

    @Setting
    public void setLightDirection(Vector3f lightDirection) {
        lightDirectionUniform.set(lightDirection);
    }

    @Setting
    public void setKernelSize(int kernelSize) {
        // Generate the kernel
        final Vector2f[] kernel = new Vector2f[kernelSize];
        final Random random = new Random();
        for (int i = 0; i < kernelSize; i++) {
            // Create a set of random unit vectors
            kernel[i] = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
        }
        // Update the uniforms
        kernelSizeUniform.set(kernelSize);
        kernelUniform.set(kernel);
    }

    @Setting
    public void setRadius(float radius) {
        radiusUniform.set(radius);
    }

    @Setting
    public void setBias(float bias) {
        biasUniform.set(bias);
    }

    @Setting
    public void setNoiseSize(int noiseSize) {
        // Generate the noise texture data
        final Random random = new Random();
        final int noiseTextureSize = noiseSize * noiseSize;
        final ByteBuffer noiseTextureBuffer = CausticUtil.createByteBuffer(noiseTextureSize * 2);
        for (int i = 0; i < noiseTextureSize; i++) {
            // Random unit vectors around the z axis
            Vector2f noise = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
            // Encode to unsigned byte, and place in buffer
            noise = noise.mul(128).add(128, 128);
            noiseTextureBuffer.put((byte) (noise.getFloorX() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorY() & 0xff));
        }
        // Update the uniform
        noiseScaleUniform.set(new Vector2f(graph.getWindowWidth(), graph.getWindowHeight()).div(noiseSize));
        // Update the texture
        noiseTextureBuffer.flip();
        noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
    }

    /**
     * Updates the light direction and camera bounds to ensure that shadows are casted inside the frustum.
     *
     * @param direction The light direction
     * @param frustum The frustum in which to cast shadows
     */
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
        Vector3f position = frustum.getPosition();
        final Vector3f[] vertices = frustum.getVertices();
        final Vector3f p0 = axisAlignTransform.transform(vertices[0].sub(position));
        final Vector3f p1 = axisAlignTransform.transform(vertices[1].sub(position));
        final Vector3f p2 = axisAlignTransform.transform(vertices[2].sub(position));
        final Vector3f p3 = axisAlignTransform.transform(vertices[3].sub(position));
        final Vector3f p4 = axisAlignTransform.transform(vertices[4].sub(position));
        final Vector3f p5 = axisAlignTransform.transform(vertices[5].sub(position));
        final Vector3f p6 = axisAlignTransform.transform(vertices[6].sub(position));
        final Vector3f p7 = axisAlignTransform.transform(vertices[7].sub(position));
        // Calculate the new camera bounds so that the box is fully included in those bounds
        final Vector3f low = p0.min(p1).min(p2).min(p3)
                .min(p4).min(p5).min(p6).min(p7);
        final Vector3f high = p0.max(p1).max(p2).max(p3)
                .max(p4).max(p5).max(p6).max(p7);
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

    @Input("normals")
    public void setNormalsInput(Texture texture) {
        texture.checkCreated();
        normalsInput = texture;
    }

    @Input("depths")
    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        depthsInput = texture;
    }

    @Output("shadows")
    public Texture getShadowsOutput() {
        return shadowsOutput;
    }

    protected class RenderShadowModelsAction extends Action {
        private final Material material;
        private final Collection<Model> models;

        protected RenderShadowModelsAction(Collection<Model> models) {
            this.material = new Material(graph.getProgram("basic"));
            this.models = models;
        }

        @Override
        public void execute(Context context) {
            final Program program = material.getProgram();
            // Bind the material
            material.bind();
            // Upload the camera matrices
            final Camera camera = context.getCamera();
            program.setUniform("projectionMatrix", camera.getProjectionMatrix());
            program.setUniform("viewMatrix", camera.getViewMatrix());
            for (Model model : models) {
                // Upload the model and normal matrices
                program.setUniform("modelMatrix", model.getMatrix());
                program.setUniform("normalMatrix", camera.getViewMatrix().mul(model.getMatrix()).invert().transpose());
                // Render the model
                model.render();
            }
        }
    }
}