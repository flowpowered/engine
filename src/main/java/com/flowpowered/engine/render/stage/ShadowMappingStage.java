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
/**
 * This file is part of Client, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spoutcraft <http://spoutcraft.org/>
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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;

import com.flowpowered.engine.render.FlowRenderer;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.Creatable;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector2ArrayUniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
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

// TODO: cascaded shadow maps, render models for light depths using the basic shader
public class ShadowMappingStage extends Creatable {
    private final FlowRenderer renderer;
    private final Material material;
    private final Texture lightDepthsTexture;
    private final Texture noiseTexture;
    private final FrameBuffer depthFrameBuffer;
    private final FrameBuffer frameBuffer;
    private Texture normalsInput;
    private Texture depthsInput;
    private Texture shadowsOutput;
    private final Matrix4Uniform inverseViewMatrixUniform = new Matrix4Uniform("inverseViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightViewMatrixUniform = new Matrix4Uniform("lightViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightProjectionMatrixUniform = new Matrix4Uniform("lightProjectionMatrix", new Matrix4f());
    private final Camera camera = Camera.createOrthographic(50, -50, 50, -50, -50, 50);
    private Pipeline pipeline;
    private int kernelSize = 8;
    private int noiseSize = 4;
    private float bias = 0.005f;
    private float radius = 0.0004f;

    public ShadowMappingStage(FlowRenderer renderer) {
        this.renderer = renderer;
        material = new Material(renderer.getProgram("shadow"));
        final GLFactory glFactory = renderer.getGLFactory();
        lightDepthsTexture = glFactory.createTexture();
        noiseTexture = glFactory.createTexture();
        depthFrameBuffer = glFactory.createFrameBuffer();
        frameBuffer = glFactory.createFrameBuffer();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Shadow mapping has already been created");
        }
        // Generate the kernel
        final Vector2f[] kernel = new Vector2f[kernelSize];
        final Random random = new Random();
        for (int i = 0; i < kernelSize; i++) {
            // Create a set of random unit vectors
            kernel[i] = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
        }
        // Generate the noise texture data
        final int noiseTextureSize = noiseSize * noiseSize;
        final ByteBuffer noiseTextureBuffer = CausticUtil.createByteBuffer(noiseTextureSize * 3);
        for (int i = 0; i < noiseTextureSize; i++) {
            // Random unit vectors around the z axis
            Vector2f noise = new Vector2f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
            // Encode to unsigned byte, and place in buffer
            noise = noise.mul(128).add(128, 128);
            noiseTextureBuffer.put((byte) (noise.getFloorX() & 0xff));
            noiseTextureBuffer.put((byte) (noise.getFloorY() & 0xff));
            noiseTextureBuffer.put((byte) 0);
        }
        // Create the noise texture
        noiseTexture.setFormat(Format.RGB);
        noiseTexture.setInternalFormat(InternalFormat.RGB8);
        noiseTextureBuffer.flip();
        noiseTexture.setImageData(noiseTextureBuffer, noiseSize, noiseSize);
        noiseTexture.create();
        // Create the depth texture
        lightDepthsTexture.setFormat(Format.DEPTH);
        lightDepthsTexture.setInternalFormat(InternalFormat.DEPTH_COMPONENT32);
        lightDepthsTexture.setWrapS(WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture.setWrapT(WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture.setMagFilter(FilterMode.LINEAR);
        lightDepthsTexture.setMinFilter(FilterMode.LINEAR);
        lightDepthsTexture.setCompareMode(CompareMode.LESS);
        lightDepthsTexture.setImageData(null, FlowRenderer.SHADOW_SIZE.getFloorX(), FlowRenderer.SHADOW_SIZE.getFloorY());
        lightDepthsTexture.create();
        // Create the material
        material.addTexture(0, normalsInput);
        material.addTexture(1, depthsInput);
        material.addTexture(2, lightDepthsTexture);
        material.addTexture(3, noiseTexture);
        final UniformHolder uniforms = material.getUniforms();
        uniforms.add(new Vector2Uniform("projection", FlowRenderer.PROJECTION));
        uniforms.add(new FloatUniform("tanHalfFOV", FlowRenderer.TAN_HALF_FOV));
        uniforms.add(new FloatUniform("aspectRatio", FlowRenderer.ASPECT_RATIO));
        uniforms.add(renderer.getLightDirectionUniform());
        uniforms.add(inverseViewMatrixUniform);
        uniforms.add(lightViewMatrixUniform);
        uniforms.add(lightProjectionMatrixUniform);
        uniforms.add(new IntUniform("kernelSize", kernelSize));
        uniforms.add(new Vector2ArrayUniform("kernel", kernel));
        uniforms.add(new Vector2Uniform("noiseScale", new Vector2f(shadowsOutput.getWidth(), shadowsOutput.getHeight()).div(noiseSize)));
        uniforms.add(new FloatUniform("bias", bias));
        uniforms.add(new FloatUniform("radius", radius));
        // Create the screen model
        final Model model = new Model(renderer.getScreen(), material);
        // Create the depth frame buffer
        depthFrameBuffer.attach(AttachmentPoint.DEPTH, lightDepthsTexture);
        depthFrameBuffer.create();
        // Create the frame buffer
        frameBuffer.attach(AttachmentPoint.COLOR0, shadowsOutput);
        frameBuffer.create();
        // Create the pipeline
        final RenderModelsStage renderModelsStage = renderer.getRenderModelsStage();
        pipeline = new PipelineBuilder().useViewPort(new Rectangle(Vector2f.ZERO, FlowRenderer.SHADOW_SIZE)).useCamera(camera).bindFrameBuffer(depthFrameBuffer).clearBuffer()
                .renderModels(renderModelsStage.getModels()).useViewPort(new Rectangle(Vector2f.ZERO, FlowRenderer.WINDOW_SIZE)).useCamera(renderModelsStage.getCamera())
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
        if (shadowsOutput.isCreated()) {
            shadowsOutput.destroy();
        }
        super.destroy();
    }

    public void render() {
        inverseViewMatrixUniform.set(renderer.getRenderModelsStage().getCamera().getViewMatrix().invert());
        lightViewMatrixUniform.set(camera.getViewMatrix());
        lightProjectionMatrixUniform.set(camera.getProjectionMatrix());
        pipeline.run(renderer.getContext());
    }

    public void setKernelSize(int kernelSize) {
        this.kernelSize = kernelSize;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setBias(float bias) {
        this.bias = bias;
    }

    public void setNoiseSize(int noiseSize) {
        this.noiseSize = noiseSize;
    }

    public void setNormalsInput(Texture texture) {
        texture.checkCreated();
        normalsInput = texture;
    }

    public void setDepthsInput(Texture texture) {
        texture.checkCreated();
        depthsInput = texture;
    }

    public void setShadowsOutput(Texture texture) {
        texture.checkCreated();
        shadowsOutput = texture;
    }

    public Texture getShadowsOutput() {
        return shadowsOutput;
    }

    public Camera getCamera() {
        return camera;
    }
}
