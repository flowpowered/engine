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
package org.spout.engine.render;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flowpowered.commons.TPSMonitor;

import org.lwjgl.opengl.GLContext;
import org.spout.api.render.Renderer;
import org.spout.engine.render.effect.BlurEffect;
import org.spout.engine.render.effect.SSAOEffect;
import org.spout.engine.render.effect.ShadowMappingEffect;
import org.spout.engine.scheduler.SpoutScheduler;

import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;
import org.spout.renderer.api.Action.RenderModelsAction;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLImplementation;
import org.spout.renderer.api.GLVersioned.GLVersion;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.Color;
import org.spout.renderer.api.data.Uniform;
import org.spout.renderer.api.data.Uniform.ColorUniform;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.data.VertexAttribute.DataType;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.Context.BlendFunction;
import org.spout.renderer.api.gl.Context.Capability;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Shader;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.CompareMode;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.model.StringModel;
import org.spout.renderer.api.util.MeshGenerator;
import org.spout.renderer.api.util.Rectangle;
import org.spout.renderer.lwjgl.LWJGLUtil;

/**
 * The default renderer. Support OpenGL 2.1 and 3.2. Can render fully textured models with normal and specular mapping, ambient occlusion (SSAO), shadow mapping, Phong shading, motion blur and edge
 * detection anti-aliasing. The default OpenGL version is 3.2.
 */
public class SpoutRenderer implements Renderer {
    // CONSTANTS
    private final String WINDOW_TITLE = "Spout";
    private final Vector2f WINDOW_SIZE = new Vector2f(1200, 800);
    private final Vector2f SHADOW_SIZE = new Vector2f(2048, 2048);
    private final float ASPECT_RATIO = WINDOW_SIZE.getX() / WINDOW_SIZE.getY();
    private final float FIELD_OF_VIEW = 60;
    private final float TAN_HALF_FOV = (float) Math.tan(Math.toRadians(FIELD_OF_VIEW) / 2);
    private final float NEAR_PLANE = 0.1f;
    private final float FAR_PLANE = 1000;
    private final Vector2f PROJECTION = new Vector2f(FAR_PLANE / (FAR_PLANE - NEAR_PLANE), (-FAR_PLANE * NEAR_PLANE) / (FAR_PLANE - NEAR_PLANE));
    private final Pattern ATTACHMENT_PATTERN = Pattern.compile("([a-zA-Z]+)(\\d*)");
    private final DateFormat SCREENSHOT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    // SETTINGS
    private boolean cullBackFaces = true;
    // EFFECT UNIFORMS
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", Vector3f.FORWARD);
    private final Matrix4Uniform inverseViewMatrixUniform = new Matrix4Uniform("inverseViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightViewMatrixUniform = new Matrix4Uniform("lightViewMatrix", new Matrix4f());
    private final Matrix4Uniform lightProjectionMatrixUniform = new Matrix4Uniform("lightProjectionMatrix", new Matrix4f());
    private final Matrix4Uniform previousViewMatrixUniform = new Matrix4Uniform("previousViewMatrix", new Matrix4f());
    private final Matrix4Uniform previousProjectionMatrixUniform = new Matrix4Uniform("previousProjectionMatrix", new Matrix4f());
    private final FloatUniform blurStrengthUniform = new FloatUniform("blurStrength", 1);
    // CAMERAS
    private final Camera modelCamera = Camera.createPerspective(FIELD_OF_VIEW, WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), NEAR_PLANE, FAR_PLANE);
    private final Camera lightCamera = Camera.createOrthographic(50, -50, 50, -50, -50, 50);
    private final Camera guiCamera = Camera.createOrthographic(1, 0, 1 / ASPECT_RATIO, 0, NEAR_PLANE, FAR_PLANE);
    // OPENGL VERSION AND FACTORY
    private GLVersion glVersion;
    private GLFactory glFactory;
    // CONTEXT
    private Context context;
    // RENDER LISTS
    private final List<Model> modelRenderList = new ArrayList<>();
    private final List<Model> transparentModelList = new ArrayList<>();
    private final List<Model> guiRenderList = new ArrayList<>();
    // PIPELINE
    private Pipeline pipeline;
    // SHADERS
    private final Map<String, Program> programs = new HashMap<>();
    // TEXTURES
    private final Map<String, Texture> textures = new HashMap<>();
    // MATERIALS
    private final Map<String, Material> materials = new HashMap<>();
    // FRAME BUFFERS
    private final Map<String, FrameBuffer> frameBuffers = new HashMap<>();
    // VERTEX ARRAYS
    private VertexArray deferredStageScreenVertexArray;
    // EFFECTS
    private SSAOEffect ssaoEffect;
    private ShadowMappingEffect shadowMappingEffect;
    private BlurEffect blurEffect;
    // MODEL PROPERTIES
    private Color solidModelColor;
    // FPS MONITOR
    private final TPSMonitor fpsMonitor = new TPSMonitor();
    private StringModel fpsMonitorModel;
    private boolean fpsMonitorStarted = false;

    public SpoutRenderer() {
        setGLVersion(GLVersion.GL32);
    }

    /**
     * Creates the OpenGL context and initializes the internal resources for the renderer
     */
    public void init() {
        initContext();
        initEffects();
        initPrograms();
        initTextures();
        initMaterials();
        initFrameBuffers();
        initVertexArrays();
        initPipeline();
        addDefaultObjects();
    }

    private void initContext() {
        // CONTEXT
        context = glFactory.createContext();
        context.setWindowTitle(WINDOW_TITLE);
        context.setWindowSize(WINDOW_SIZE);
        context.create();
        context.setClearColor(new Color(0, 0, 0, 0));
        context.setCamera(modelCamera);
        if (cullBackFaces) {
            context.enableCapability(Capability.CULL_FACE);
        }
        context.enableCapability(Capability.DEPTH_TEST);
        if (glVersion == GLVersion.GL30 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            context.enableCapability(Capability.DEPTH_CLAMP);
        }
        final UniformHolder uniforms = context.getUniforms();
        uniforms.add(previousViewMatrixUniform);
        uniforms.add(previousProjectionMatrixUniform);
    }

    private void initEffects() {
        final int blurSize = 2;
        // SSAO
        ssaoEffect = new SSAOEffect(glFactory, WINDOW_SIZE, 8, blurSize, 0.5f, 0.15f, 2);
        // SHADOW MAPPING
        shadowMappingEffect = new ShadowMappingEffect(glFactory, WINDOW_SIZE, 8, blurSize, 0.005f, 0.0004f);
        // BLUR
        blurEffect = new BlurEffect(WINDOW_SIZE, blurSize);
    }

    private void initPipeline() {
        PipelineBuilder pipelineBuilder = new PipelineBuilder();
        // MODEL
        pipelineBuilder = pipelineBuilder
                .bindFrameBuffer(frameBuffers.get("model"))
                .clearBuffer()
                .renderModels(modelRenderList);
        // WEIGHTED SUM TRANSPARENCY
        pipelineBuilder = pipelineBuilder
                .disableDepthMask()
                .disableCapabilities(Capability.CULL_FACE)
                .enableCapabilities(Capability.BLEND)
                .setBlendingFunctions(BlendFunction.GL_ONE, BlendFunction.GL_ONE)
                .bindFrameBuffer(frameBuffers.get("weightedSum"))
                .clearBuffer()
                .renderModels(transparentModelList)
                .disableCapabilities(Capability.BLEND)
                .enableCapabilities(Capability.CULL_FACE)
                .enableDepthMask();
        // LIGHT MODEL
        pipelineBuilder = pipelineBuilder
                .useViewPort(new Rectangle(Vector2f.ZERO, SHADOW_SIZE))
                .useCamera(lightCamera)
                .bindFrameBuffer(frameBuffers.get("lightModel"))
                .clearBuffer()
                .renderModels(modelRenderList)
                .useViewPort(new Rectangle(Vector2f.ZERO, WINDOW_SIZE))
                .useCamera(modelCamera);
        // SSAO
        if (glVersion == GLVersion.GL30 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            pipelineBuilder = pipelineBuilder
                    .disableCapabilities(Capability.DEPTH_CLAMP);
        }
        pipelineBuilder = pipelineBuilder
                .disableCapabilities(Capability.DEPTH_TEST)
                .doAction(new DoDeferredStageAction("ssao", deferredStageScreenVertexArray, "ssao"));
        // SHADOW
        pipelineBuilder = pipelineBuilder
                .doAction(new DoDeferredStageAction("shadow", deferredStageScreenVertexArray, "shadow"));
        // BLUR
        pipelineBuilder = pipelineBuilder
                .doAction(new DoDeferredStageAction("blur", deferredStageScreenVertexArray, "blur"));
        // LIGHTING
        pipelineBuilder = pipelineBuilder
                .doAction(new DoDeferredStageAction("lighting", deferredStageScreenVertexArray, "lighting"));
        // ANTI ALIASING
        pipelineBuilder = pipelineBuilder
                .doAction(new DoDeferredStageAction("antiAliasing", deferredStageScreenVertexArray, "antiAliasing"));
        // TRANSPARENCY BLENDING
        pipelineBuilder = pipelineBuilder
                .enableCapabilities(Capability.BLEND)
                .setBlendingFunctions(BlendFunction.GL_ONE_MINUS_SRC_ALPHA, BlendFunction.GL_SRC_ALPHA)
                .doAction(new DoDeferredStageAction("transparencyBlending", deferredStageScreenVertexArray, "transparencyBlending"))
                .disableCapabilities(Capability.BLEND)
                .enableDepthMask();
        // MOTION BLUR
        pipelineBuilder = pipelineBuilder
                .doAction(new DoDeferredStageAction("motionBlur", deferredStageScreenVertexArray, "motionBlur"))
                .unbindFrameBuffer(frameBuffers.get("motionBlur"))
                .enableCapabilities(Capability.DEPTH_TEST);
        if (glVersion == GLVersion.GL30 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            pipelineBuilder = pipelineBuilder
                    .enableCapabilities(Capability.DEPTH_CLAMP);
        }
        // GUI
        pipelineBuilder = pipelineBuilder
                .useCamera(guiCamera)
                .clearBuffer()
                .renderModels(guiRenderList)
                .useCamera(modelCamera)
                .updateDisplay();
        // BUILD
        pipeline = pipelineBuilder.build();
    }

    private void initPrograms() {
        // SOLID
        loadProgram("solid");
        // TEXTURED
        loadProgram("textured");
        /// FONT
        loadProgram("font");
        // SSAO
        loadProgram("ssao");
        // SHADOW
        loadProgram("shadow");
        // BLUR
        loadProgram("blur");
        // LIGHTING
        loadProgram("lighting");
        // MOTION BLUR
        loadProgram("motionBlur");
        // ANTI ALIASING
        loadProgram("edaa");
        // WEIGHTED SUM
        loadProgram("weightedSum");
        // TRANSPARENCY BLENDING
        loadProgram("transparencyBlending");
        // SCREEN
        loadProgram("screen");
    }

    private void loadProgram(String name) {
        final String shaderPath = "/shaders/" + glVersion.toString().toLowerCase() + "/" + name;
        // SHADERS
        final Shader vert = glFactory.createShader();
        vert.setSource(SpoutRenderer.class.getResourceAsStream(shaderPath + ".vert"));
        vert.create();
        final Shader frag = glFactory.createShader();
        frag.setSource(SpoutRenderer.class.getResourceAsStream(shaderPath + ".frag"));
        frag.create();
        // PROGRAM
        final Program program = glFactory.createProgram();
        program.addShader(vert);
        program.addShader(frag);
        program.create();
        programs.put(name, program);
    }

    private void initTextures() {
        Texture texture;
        // COLORS
        texture = createTexture("colors", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGBA, InternalFormat.RGBA8);
        texture.setWrapS(WrapMode.CLAMP_TO_EDGE);
        texture.setWrapT(WrapMode.CLAMP_TO_EDGE);
        texture.setMagFilter(FilterMode.LINEAR);
        texture.setMinFilter(FilterMode.LINEAR);
        texture.create();
        // NORMALS
        texture = createTexture("normals", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGBA, InternalFormat.RGBA8);
        texture.create();
        // VERTEX NORMALS
        texture = createTexture("vertexNormals", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGBA, InternalFormat.RGBA8);
        texture.create();
        // MATERIALS
        texture = createTexture("materials", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGB, InternalFormat.RGB8);
        texture.create();
        // VELOCITIES
        texture = createTexture("velocities", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RG, InternalFormat.RG16F);
        texture.setComponentType(DataType.HALF_FLOAT);
        texture.create();
        // DEPTHS
        texture = createTexture("depths", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.DEPTH, InternalFormat.DEPTH_COMPONENT32);
        texture.setWrapS(WrapMode.CLAMP_TO_EDGE);
        texture.setWrapT(WrapMode.CLAMP_TO_EDGE);
        texture.create();
        // LIGHT DEPTHS
        texture = createTexture("lightDepths", SHADOW_SIZE.getFloorX(), SHADOW_SIZE.getFloorY(), Format.DEPTH, InternalFormat.DEPTH_COMPONENT32);
        texture.setWrapS(WrapMode.CLAMP_TO_BORDER);
        texture.setWrapT(WrapMode.CLAMP_TO_BORDER);
        texture.setMagFilter(FilterMode.LINEAR);
        texture.setMinFilter(FilterMode.LINEAR);
        texture.setCompareMode(CompareMode.LESS);
        texture.create();
        // SSAO
        texture = createTexture("ssao", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RED, InternalFormat.R8);
        texture.create();
        // SHADOW
        texture = createTexture("shadow", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RED, InternalFormat.R8);
        texture.create();
        // AUX R
        texture = createTexture("auxR", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RED, InternalFormat.R8);
        texture.create();
        // AUX RGBA
        texture = createTexture("auxRGBA", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGBA, InternalFormat.RGBA8);
        texture.setMagFilter(FilterMode.LINEAR);
        texture.setMinFilter(FilterMode.LINEAR);
        texture.create();
        // WEIGHTED COLORS
        texture = createTexture("weightedColors", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RGBA, InternalFormat.RGBA16F);
        texture.create();
        // WEIGHTED VELOCITIES
        texture = createTexture("weightedVelocities", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RG, InternalFormat.RG16F);
        texture.create();
        // LAYER COUNTS
        texture = createTexture("layerCounts", WINDOW_SIZE.getFloorX(), WINDOW_SIZE.getFloorY(), Format.RED, InternalFormat.R16F);
        texture.create();
    }

    private Texture createTexture(String name, int width, int height, Format format, InternalFormat internalFormat) {
        final Texture texture = glFactory.createTexture();
        texture.setFormat(format);
        texture.setInternalFormat(internalFormat);
        texture.setImageData(null, width, height);
        textures.put(name, texture);
        return texture;
    }

    private void initMaterials() {
        Material material;
        // SOLID
        material = createMaterial("solid", "solid");
        addUniforms(material, new FloatUniform("diffuseIntensity", 0.8f), new FloatUniform("specularIntensity", 1), new FloatUniform("ambientIntensity", 0.2f));
        // SSAO
        material = createMaterial("ssao", "ssao", "0:normals", "1:depths");
        material.addTexture(2, ssaoEffect.getNoiseTexture());
        addUniforms(material, new Vector2Uniform("projection", PROJECTION), new FloatUniform("tanHalfFOV", TAN_HALF_FOV), new FloatUniform("aspectRatio", ASPECT_RATIO));
        addUniforms(material, ssaoEffect.getUniforms());
        // SHADOW
        material = createMaterial("shadow", "shadow", "0:vertexNormals", "1:depths", "2:lightDepths");
        material.addTexture(3, shadowMappingEffect.getNoiseTexture());
        addUniforms(material, new Vector2Uniform("projection", PROJECTION), new FloatUniform("tanHalfFOV", TAN_HALF_FOV), new FloatUniform("aspectRatio", ASPECT_RATIO),
                lightDirectionUniform, inverseViewMatrixUniform, lightViewMatrixUniform, lightProjectionMatrixUniform);
        addUniforms(material, shadowMappingEffect.getUniforms());
        // BLUR
        material = createMaterial("blur", "blur", "0:auxR", "1:auxRGBA");
        addUniforms(material, blurEffect.getUniforms());
        // LIGHTING
        material = createMaterial("lighting", "lighting", "0:colors", "1:normals", "2:depths", "3:materials", "4:ssao", "5:shadow");
        addUniforms(material, new FloatUniform("tanHalfFOV", TAN_HALF_FOV), new FloatUniform("aspectRatio", ASPECT_RATIO), lightDirectionUniform);
        // ANTI ALIASING
        material = createMaterial("antiAliasing", "edaa", "0:auxRGBA", "1:vertexNormals", "2:depths");
        addUniforms(material, new Vector2Uniform("projection", PROJECTION), new Vector2Uniform("resolution", WINDOW_SIZE), new FloatUniform("maxSpan", 8),
                new Vector2Uniform("barriers", new Vector2f(0.8f, 0.5f)), new Vector2Uniform("weights", new Vector2f(0.25f, 0.6f)), new FloatUniform("kernel", 0.75f));
        // TRANSPARENCY
        material = createMaterial("transparency", "weightedSum");
        addUniforms(material, lightDirectionUniform, new FloatUniform("diffuseIntensity", 0.8f), new FloatUniform("specularIntensity", 1), new FloatUniform("ambientIntensity", 0.2f));
        // TRANSPARENCY BLENDING
        createMaterial("transparencyBlending", "transparencyBlending", "0:weightedColors", "1:weightedVelocities", "2:layerCounts");
        // MOTION BLUR
        material = createMaterial("motionBlur", "motionBlur", "0:colors", "1:velocities");
        addUniforms(material, new Vector2Uniform("resolution", WINDOW_SIZE), new IntUniform("sampleCount", 8), blurStrengthUniform);
        // SCREEN
        createMaterial("screen", "screen", "0:auxRGBA");
    }

    private Material createMaterial(String name, String program, String... textures) {
        final Material material = new Material(programs.get(program));
        for (String texture : textures) {
            final String[] indexAndName = texture.split(":");
            material.addTexture(Integer.parseInt(indexAndName[0]), this.textures.get(indexAndName[1]));
        }
        materials.put(name, material);
        return material;
    }

    private void addUniforms(Material material, Uniform... uniforms) {
        for (Uniform uniform : uniforms) {
            material.getUniforms().add(uniform);
        }
    }

    private void initFrameBuffers() {
        // MODEL
        createFrameBuffer("model", "C0:colors", "C1:normals", "C2:vertexNormals", "C3:materials", "C4:velocities", "D:depths");
        // LIGHT MODEL
        createFrameBuffer("lightModel", "D:lightDepths");
        // SSAO
        createFrameBuffer("ssao", "C0:auxR");
        // SHADOW
        createFrameBuffer("shadow", "C0:auxRGBA");
        // BLUR
        createFrameBuffer("blur", "C0:ssao", "C1:shadow");
        // LIGHTING
        createFrameBuffer("lighting", "C0:auxRGBA");
        // ANTI ALIASING
        createFrameBuffer("antiAliasing", "C0:colors");
        // WEIGHTED SUM
        createFrameBuffer("weightedSum", "C0:weightedColors", "C1:weightedVelocities", "C2:layerCounts", "D:depths");
        // TRANSPARENCY BLENDING
        createFrameBuffer("transparencyBlending", "C0:colors", "C1:velocities");
        // MOTION BLUR
        createFrameBuffer("motionBlur", "C0:auxRGBA");
    }

    private FrameBuffer createFrameBuffer(String name, String... textures) {
        final FrameBuffer frameBuffer = glFactory.createFrameBuffer();
        for (String texture : textures) {
            final String[] typeAndName = texture.split(":");
            frameBuffer.attach(decodeAttachment(typeAndName[0]), this.textures.get(typeAndName[1]));
        }
        frameBuffer.create();
        frameBuffers.put(name, frameBuffer);
        return frameBuffer;
    }

    private void initVertexArrays() {
        // DEFERRED STAGE SCREEN
        deferredStageScreenVertexArray = glFactory.createVertexArray();
        deferredStageScreenVertexArray.setData(MeshGenerator.generateTexturedPlane(null, new Vector2f(2, 2)));
        deferredStageScreenVertexArray.create();
    }

    private void addDefaultObjects() {
        addScreen();
        addFPSMonitor();
    }

    /**
     * Destroys the renderer internal resources and the OpenGL context.
     */
    public void dispose() {
        disposeEffects();
        disposePrograms();
        disposeTextures();
        disposeMaterials();
        disposeFrameBuffers();
        disposeVertexArrays();
        disposeContext();
        fpsMonitorStarted = false;
    }

    private void disposeContext() {
        // CONTEXT
        context.destroy();
    }

    private void disposeEffects() {
        // SSAO
        ssaoEffect.dispose();
        // SHADOW MAPPING
        shadowMappingEffect.dispose();
        // BLUR
        blurEffect.dispose();
    }

    private void disposePrograms() {
        for (Program program : programs.values()) {
            // SHADERS
            for (Shader shader : program.getShaders()) {
                shader.destroy();
            }
            // PROGRAM
            program.destroy();
        }
    }

    private void disposeTextures() {
        for (Texture texture : textures.values()) {
            texture.destroy();
        }
    }

    private void disposeMaterials() {
        materials.clear();
    }

    private void disposeFrameBuffers() {
        for (FrameBuffer frameBuffer : frameBuffers.values()) {
            frameBuffer.destroy();
        }
    }

    private void disposeVertexArrays() {
        // DEFERRED STAGE SCREEN
        deferredStageScreenVertexArray.destroy();
    }

    /**
     * Sets the OpenGL version. Must be done before initializing the renderer.
     *
     * @param version The OpenGL version to use
     */
    public void setGLVersion(GLVersion version) {
        switch (version) {
            case GL20:
            case GL21:
                glFactory = GLImplementation.get(LWJGLUtil.GL21_IMPL);
                glVersion = GLVersion.GL21;
                break;
            case GL30:
            case GL31:
            case GL32:
                glFactory = GLImplementation.get(LWJGLUtil.GL32_IMPL);
                glVersion = GLVersion.GL32;
        }
    }

    /**
     * Returns the OpenGL version.
     *
     * @return The OpenGL version
     */
    public GLVersion getGLVersion() {
        return glVersion;
    }

    /**
     * Returns the OpenGL factory.
     *
     * @return The OpenGL factory
     */
    public GLFactory getGLFactory() {
        return glFactory;
    }

    /**
     * Sets whether or not to cull the back faces of the geometry.
     *
     * @param cull Whether or not to cull the back faces
     */
    public void setCullBackFaces(boolean cull) {
        cullBackFaces = cull;
    }

    /**
     * Sets the color of solid untextured objects.
     *
     * @param color The solid color
     */
    public void setSolidColor(Color color) {
        solidModelColor = color;
    }

    /**
     * Returns the renderer camera
     *
     * @return The camera
     */
    public Camera getCamera() {
        return modelCamera;
    }

    /**
     * Updates the light direction and camera bounds to ensure that shadows are casted inside the cuboid defined by size.
     *
     * @param direction The light direction
     * @param position The light camera position
     * @param size The size of the cuboid that must have shadows
     */
    public void updateLight(Vector3f direction, Vector3f position, Vector3f size) {
        // Set the direction uniform
        direction = direction.normalize();
        lightDirectionUniform.set(direction);
        // Set the camera position
        lightCamera.setPosition(position);
        // Calculate the camera rotation from the direction and set
        final Quaternionf rotation = Quaternionf.fromRotationTo(Vector3f.FORWARD.negate(), direction);
        lightCamera.setRotation(rotation);
        // Calculate the transformation from the camera bounds rotation to the identity rotation (its axis aligned space)
        final Matrix3f axisAlignTransform = Matrix3f.createRotation(rotation).invert();
        // Calculate the points of the box to completely include inside the camera bounds
        size = size.div(2);
        Vector3f p6 = size;
        Vector3f p0 = p6.negate();
        Vector3f p7 = new Vector3f(-size.getX(), size.getY(), size.getZ());
        Vector3f p1 = p7.negate();
        Vector3f p4 = new Vector3f(-size.getX(), size.getY(), -size.getZ());
        Vector3f p2 = p4.negate();
        Vector3f p5 = new Vector3f(size.getX(), size.getY(), -size.getZ());
        Vector3f p3 = p5.negate();
        // Transform those points to the axis aligned space of the camera bounds
        p0 = axisAlignTransform.transform(p0);
        p1 = axisAlignTransform.transform(p1);
        p2 = axisAlignTransform.transform(p2);
        p3 = axisAlignTransform.transform(p3);
        p4 = axisAlignTransform.transform(p4);
        p5 = axisAlignTransform.transform(p5);
        p6 = axisAlignTransform.transform(p6);
        p7 = axisAlignTransform.transform(p7);
        // Calculate the new camera bounds so that the box is fully included in those bounds
        final Vector3f low = p0.min(p1).min(p2).min(p3)
                .min(p4).min(p5).min(p6).min(p7);
        final Vector3f high = p0.max(p1).max(p2).max(p3)
                .max(p4).max(p5).max(p6).max(p7);
        // Calculate the size of the new camera bounds
        size = high.sub(low).div(2);
        // Update the camera to the new bounds
        lightCamera.setProjection(Matrix4f.createOrthographic(size.getX(), -size.getX(), size.getY(), -size.getY(), -size.getZ(), size.getZ()));
    }

    /**
     * Adds a model to be rendered as a solid.
     *
     * @param model The model
     */
    public void addSolidModel(Model model) {
        model.setMaterial(materials.get("solid"));
        model.getUniforms().add(new ColorUniform("modelColor", solidModelColor));
        addModel(model);
    }

    /**
     * Adds a model to be rendered as partially transparent.
     *
     * @param model The transparent model
     */
    public void addTransparentModel(Model model) {
        model.setMaterial(materials.get("transparency"));
        model.getUniforms().add(new Matrix4Uniform("previousModelMatrix", model.getMatrix()));
        transparentModelList.add(model);
    }

    /**
     * Adds a model to the renderer.
     *
     * @param model The model to add
     */
    public void addModel(Model model) {
        model.getUniforms().add(new Matrix4Uniform("previousModelMatrix", model.getMatrix()));
        modelRenderList.add(model);
    }

    /**
     * Removes a model from the renderer.
     *
     * @param model The model to remove
     */
    public void removeModel(Model model) {
        modelRenderList.remove(model);
    }

    /**
     * Removes all the models from the renderer.
     */
    public void clearModels() {
        modelRenderList.clear();
    }

    /**
     * Returns the modifiable list of the models. Changes in this list are reflected in the renderer.
     *
     * @return The modifiable list of models
     */
    public List<Model> getModels() {
        return modelRenderList;
    }

    private void addScreen() {
        guiRenderList.add(new Model(deferredStageScreenVertexArray, materials.get("screen")));
    }

    private void addFPSMonitor() {
        final Font ubuntu;
        try {
            ubuntu = Font.createFont(Font.TRUETYPE_FONT, SpoutRenderer.class.getResourceAsStream("/fonts/ubuntu-r.ttf"));
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return;
        }
        final StringModel sandboxModel = new StringModel(glFactory, programs.get("font"), "ClientWIPFPS0123456789-: ", ubuntu.deriveFont(Font.PLAIN, 15), WINDOW_SIZE.getFloorX());
        final float aspect = 1 / ASPECT_RATIO;
        sandboxModel.setPosition(new Vector3f(0.005, aspect / 2 + 0.315, -0.1));
        sandboxModel.setString("Client - WIP");
        guiRenderList.add(sandboxModel);
        final StringModel fpsModel = sandboxModel.getInstance();
        fpsModel.setPosition(new Vector3f(0.005, aspect / 2 + 0.285, -0.1));
        fpsModel.setString("FPS: " + fpsMonitor.getTPS());
        guiRenderList.add(fpsModel);
        fpsMonitorModel = fpsModel;
    }

    /**
     * Renders the models to the window.
     */
    public void render() {
        if (!fpsMonitorStarted) {
            fpsMonitor.start();
            fpsMonitorStarted = true;
        }
        // UPDATE PER-FRAME UNIFORMS
        inverseViewMatrixUniform.set(modelCamera.getViewMatrix().invert());
        lightViewMatrixUniform.set(lightCamera.getViewMatrix());
        lightProjectionMatrixUniform.set(lightCamera.getProjectionMatrix());
        blurStrengthUniform.set((float) fpsMonitor.getTPS() / SpoutScheduler.TARGET_FPS);
        // RENDER
        pipeline.run(context);
        // UPDATE PREVIOUS FRAME UNIFORMS
        setPreviousModelMatrices();
        previousViewMatrixUniform.set(modelCamera.getViewMatrix());
        previousProjectionMatrixUniform.set(modelCamera.getProjectionMatrix());
        // UPDATE FPS
        updateFPSMonitor();
    }

    private void setPreviousModelMatrices() {
        for (Model model : modelRenderList) {
            model.getUniforms().getMatrix4("previousModelMatrix").set(model.getMatrix());
        }
        for (Model model : transparentModelList) {
            model.getUniforms().getMatrix4("previousModelMatrix").set(model.getMatrix());
        }
    }

    private void updateFPSMonitor() {
        fpsMonitor.update();
        fpsMonitorModel.setString("FPS: " + fpsMonitor.getTPS());
    }

    /**
     * Saves a screenshot (PNG) to the directory where the program is currently running, with the current date as the file name.
     *
     * @param outputDir The directory in which to output the file
     */
    public void saveScreenshot(File outputDir) {
        final ByteBuffer buffer = context.readCurrentFrame(new Rectangle(Vector2f.ZERO, WINDOW_SIZE), Format.RGB);
        final int width = context.getWindowWidth();
        final int height = context.getWindowHeight();
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int srcIndex = (x + y * width) * 3;
                final int destIndex = (x + (height - y - 1) * width) * 3;
                data[destIndex + 2] = buffer.get(srcIndex);
                data[destIndex + 1] = buffer.get(srcIndex + 1);
                data[destIndex] = buffer.get(srcIndex + 2);
            }
        }
        try {
            ImageIO.write(image, "PNG", new File(outputDir, SCREENSHOT_DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AttachmentPoint decodeAttachment(String s) {
        final Matcher matcher = ATTACHMENT_PATTERN.matcher(s);
        if (!matcher.find()) {
            return null;
        }
        switch (matcher.group(1).toUpperCase()) {
            case "C":
                switch (Integer.parseInt(matcher.group(2))) {
                    case 0:
                        return AttachmentPoint.COLOR0;
                    case 1:
                        return AttachmentPoint.COLOR1;
                    case 2:
                        return AttachmentPoint.COLOR2;
                    case 3:
                        return AttachmentPoint.COLOR3;
                    case 4:
                        return AttachmentPoint.COLOR4;
                }
                return null;
            case "D":
                return AttachmentPoint.DEPTH;
            case "S":
                return AttachmentPoint.STENCIL;
            case "DS":
                return AttachmentPoint.DEPTH_STENCIL;
        }
        return null;
    }

    @Override
    public Vector2f getResolution() {
        return WINDOW_SIZE;
    }

    @Override
    public float getAspectRatio() {
        return ASPECT_RATIO;
    }

    private class DoDeferredStageAction extends RenderModelsAction {
        private final FrameBuffer frameBuffer;

        private DoDeferredStageAction(String frameBuffer, VertexArray screen, String material) {
            super(Arrays.asList(new Model(screen, materials.get(material))));
            this.frameBuffer = frameBuffers.get(frameBuffer);
        }

        @Override
        public void execute(Context context) {
            frameBuffer.bind();
            super.execute(context);
        }
    }
}