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

import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

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
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.GLFactory;
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

// TODO: cascaded shadow maps, render models for light depths using the basic shader
public class ShadowMappingNode extends GraphNode {
    private final Material material;
    private final Texture lightDepthsTexture;
    private final Texture noiseTexture;
    private final FrameBuffer depthFrameBuffer;
    private final FrameBuffer frameBuffer;
    private final Texture shadowsOutput;
    private Texture normalsInput;
    private Texture depthsInput;
    private final Matrix4Uniform inverseViewMatrixUniform = new Matrix4Uniform("inverseViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightViewMatrixUniform = new Matrix4Uniform("lightViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightProjectionMatrixUniform = new Matrix4Uniform("lightProjectionMatrix", new Matrix4f());
    private final Camera camera = Camera.createOrthographic(50, -50, 50, -50, -50, 50);
    private Vector2i shadowMapSize = new Vector2i(2048, 2048);
    private Pipeline pipeline;
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", Vector3f.UP.negate());
    private final IntUniform kernelSizeUniform = new IntUniform("kernelSize", 0);
    private final Vector2ArrayUniform kernelUniform = new Vector2ArrayUniform("kernel", new Vector2f[]{});
    private final Vector2Uniform noiseScaleUniform = new Vector2Uniform("noiseScale", Vector2f.ONE);
    private final FloatUniform biasUniform = new FloatUniform("bias", 0.005f);
    private final FloatUniform radiusUniform = new FloatUniform("radius", 0.0004f);

    public ShadowMappingNode(RenderGraph graph, String name) {
        super(graph, name);
        material = new Material(graph.getProgram("shadow"));
        final GLFactory glFactory = graph.getGLFactory();
        lightDepthsTexture = glFactory.createTexture();
        noiseTexture = glFactory.createTexture();
        noiseTexture.setFormat(Format.RG);
        noiseTexture.setInternalFormat(InternalFormat.RG8);
        depthFrameBuffer = glFactory.createFrameBuffer();
        frameBuffer = glFactory.createFrameBuffer();
        shadowsOutput = glFactory.createTexture();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Shadow mapping stage has already been created");
        }
        // Create the noise texture
        noiseTexture.create();
        // Create the shadows texture
        shadowsOutput.setFormat(Format.RED);
        shadowsOutput.setInternalFormat(InternalFormat.R8);
        shadowsOutput.setImageData(null, graph.getWindowWidth(), graph.getWindowHeight());
        shadowsOutput.create();
        // Create the depth texture
        lightDepthsTexture.setFormat(Format.DEPTH);
        lightDepthsTexture.setInternalFormat(InternalFormat.DEPTH_COMPONENT32);
        lightDepthsTexture.setWrapS(WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture.setWrapT(WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture.setMagFilter(FilterMode.LINEAR);
        lightDepthsTexture.setMinFilter(FilterMode.LINEAR);
        lightDepthsTexture.setCompareMode(CompareMode.LESS);
        lightDepthsTexture.setImageData(null, shadowMapSize.getX(), shadowMapSize.getY());
        lightDepthsTexture.create();
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
        depthFrameBuffer.attach(AttachmentPoint.DEPTH, lightDepthsTexture);
        depthFrameBuffer.create();
        // Create the frame buffer
        frameBuffer.attach(AttachmentPoint.COLOR0, shadowsOutput);
        frameBuffer.create();
        // Create the pipeline
        final RenderModelsNode renderModelsNode = (RenderModelsNode) graph.getNode("models");
        pipeline = new PipelineBuilder().useViewPort(new Rectangle(Vector2f.ZERO, shadowMapSize.toFloat())).useCamera(camera).bindFrameBuffer(depthFrameBuffer).clearBuffer()
                .renderModels(renderModelsNode.getModels()).useViewPort(new Rectangle(Vector2f.ZERO, graph.getWindowSize().toFloat())).useCamera(renderModelsNode.getCamera())
                .bindFrameBuffer(frameBuffer).renderModels(Arrays.asList(model)).unbindFrameBuffer(frameBuffer).build();
        // Update state to created
        super.create();
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
        boolean wasCreated = false;
        if (noiseTexture.isCreated()) {
            noiseTexture.destroy();
            wasCreated = true;
        }
        noiseTextureBuffer.flip();
        noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
        if (wasCreated) {
            noiseTexture.create();
        }
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

    public Camera getCamera() {
        return camera;
    }
}
