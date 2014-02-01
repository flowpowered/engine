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
package com.flowpowered.engine.render;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import com.flowpowered.api.render.Renderer;
import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.commons.ViewFrustum;
import com.flowpowered.engine.render.graph.RenderGraph;
import com.flowpowered.engine.render.graph.node.BlurNode;
import com.flowpowered.engine.render.graph.node.CascadedShadowMappingNode;
import com.flowpowered.engine.render.graph.node.LightingNode;
import com.flowpowered.engine.render.graph.node.RenderGUINode;
import com.flowpowered.engine.render.graph.node.RenderModelsNode;
import com.flowpowered.engine.render.graph.node.RenderTransparentModelsNode;
import com.flowpowered.engine.render.graph.node.SSAONode;
import com.flowpowered.engine.render.graph.node.ShadowMappingNode;
import com.flowpowered.engine.scheduler.FlowScheduler;
import com.flowpowered.engine.scheduler.render.RenderThread;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import org.lwjgl.opengl.GLContext;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLImplementation;
import org.spout.renderer.api.GLVersioned.GLVersion;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.Uniform.Vector4Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.Context.Capability;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.model.StringModel;
import org.spout.renderer.api.util.CausticUtil;
import org.spout.renderer.api.util.MeshGenerator;
import org.spout.renderer.api.util.Rectangle;
import org.spout.renderer.lwjgl.LWJGLUtil;

/**
 */
public class FlowRenderer implements Renderer {
    private static final String WINDOW_TITLE = "Flow Engine";
    private static final DateFormat SCREENSHOT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    // Settings
    private Vector2i windowSize = new Vector2i(1200, 800);
    private boolean cullBackFaces = true;
    // Effect uniforms
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", Vector3f.FORWARD);
    private final Matrix4Uniform previousViewMatrixUniform = new Matrix4Uniform("previousViewMatrix", new Matrix4f());
    private final Matrix4Uniform previousProjectionMatrixUniform = new Matrix4Uniform("previousProjectionMatrix", new Matrix4f());
    private final FloatUniform blurStrengthUniform = new FloatUniform("blurStrength", 1);
    // OpenGL version and context
    private Context context;
    // Included materials
    private Material solidMaterial;
    private Material transparencyMaterial;
    // Render graph
    private RenderGraph graph;
    // Graph nodes
    private RenderModelsNode renderModelsNode;
    private ShadowMappingNode shadowMappingNode;
    private LightingNode lightingNode;
    private RenderTransparentModelsNode renderTransparentModelsNode;
    private RenderGUINode renderGUINode;
    // FPS, TPS, and position monitor and models
    private final TPSMonitor fpsMonitor = new TPSMonitor();
    private StringModel fpsMonitorModel;
    private StringModel tpsMonitorModel;
    private StringModel itpsMonitorModel;
    private StringModel positionModel;
    private boolean fpsMonitorStarted = false;

    private FlowScheduler scheduler;

    public FlowRenderer() {
        setGLVersion(RenderThread.DEFAULT_VERSION);
    }

    /**
     * Creates the OpenGL context and initializes the internal resources for the renderer
     */
    public void init(FlowScheduler scheduler) {
        this.scheduler = scheduler;

        initContext();
        initGraph();
        initMaterials();
        addDefaultObjects();
    }

    private void initContext() {
        context.setWindowTitle(WINDOW_TITLE);
        context.setWindowSize(windowSize);
        context.create();
        context.setClearColor(Vector4f.ZERO);
        if (cullBackFaces) {
            context.enableCapability(Capability.CULL_FACE);
        }
        context.enableCapability(Capability.DEPTH_TEST);
        if (context.getGLVersion() == RenderThread.DEFAULT_VERSION || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            context.enableCapability(Capability.DEPTH_CLAMP);
        }
        final UniformHolder uniforms = context.getUniforms();
        uniforms.add(previousViewMatrixUniform);
        uniforms.add(previousProjectionMatrixUniform);
    }

    private void initGraph() {
        final float fov = 60;
        final Vector2f planes = new Vector2f(0.1f, 200);
        final int blurSize = 2;
        // Create the graph
        graph = new RenderGraph(context, "/shaders/gl" + context.getGLVersion().getMajor() + "0");
        graph.create();
        // Render models
        renderModelsNode = new RenderModelsNode(graph, "models");
        renderModelsNode.setOutputSize(windowSize);
        renderModelsNode.setFieldOfView(fov);
        renderModelsNode.setPlanes(planes);
        graph.addNode(renderModelsNode);
        // Shadows
        shadowMappingNode = new CascadedShadowMappingNode(graph, "shadows");
        shadowMappingNode.connect("normals", "vertexNormals", renderModelsNode);
        shadowMappingNode.connect("depths", "depths", renderModelsNode);
        shadowMappingNode.setFieldOfView(fov);
        shadowMappingNode.setPlanes(planes);
        shadowMappingNode.setShadowsSize(windowSize);
        shadowMappingNode.setShadowMapSize(new Vector2i(1048, 1048));
        shadowMappingNode.setRenderModelsNode(renderModelsNode);
        shadowMappingNode.setKernelSize(8);
        shadowMappingNode.setNoiseSize(blurSize);
        shadowMappingNode.setBias(0.001f);
        shadowMappingNode.setRadius(0.05f);
        graph.addNode(shadowMappingNode);
        // Blur shadows
        final BlurNode blurShadowsNode = new BlurNode(graph, "blurShadows");
        blurShadowsNode.connect("colors", "shadows", shadowMappingNode);
        blurShadowsNode.setColorsSize(windowSize);
        blurShadowsNode.setKernelGenerator(BlurNode.BOX_KERNEL);
        blurShadowsNode.setKernelSize(blurSize + 1);
        graph.addNode(blurShadowsNode);
        // SSAO
        final SSAONode ssaoNode = new SSAONode(graph, "ssao");
        ssaoNode.connect("normals", "normals", renderModelsNode);
        ssaoNode.connect("depths", "depths", renderModelsNode);
        ssaoNode.setFieldOfView(fov);
        ssaoNode.setPlanes(planes);
        ssaoNode.setOcclusionsSize(windowSize);
        ssaoNode.setKernelSize(8);
        ssaoNode.setThreshold(0.15f);
        ssaoNode.setNoiseSize(blurSize);
        ssaoNode.setRadius(0.5f);
        ssaoNode.setPower(2);
        graph.addNode(ssaoNode);
        // Blur occlusions
        final BlurNode blurOcclusionsNode = new BlurNode(graph, "blurOcclusions");
        blurOcclusionsNode.connect("colors", "occlusions", ssaoNode);
        blurOcclusionsNode.setColorsSize(windowSize);
        blurOcclusionsNode.setKernelGenerator(BlurNode.BOX_KERNEL);
        blurOcclusionsNode.setKernelSize(blurSize + 1);
        graph.addNode(blurOcclusionsNode);
        // Lighting
        lightingNode = new LightingNode(graph, "lighting");
        lightingNode.connect("colors", "colors", renderModelsNode);
        lightingNode.connect("normals", "normals", renderModelsNode);
        lightingNode.connect("depths", "depths", renderModelsNode);
        lightingNode.connect("materials", "materials", renderModelsNode);
        lightingNode.connect("occlusions", "colors", blurOcclusionsNode);
        lightingNode.connect("shadows", "colors", blurShadowsNode);
        lightingNode.setColorsSize(windowSize);
        lightingNode.setFieldOfView(fov);
        graph.addNode(lightingNode);
        // Transparent models
        renderTransparentModelsNode = new RenderTransparentModelsNode(graph, "transparency");
        renderTransparentModelsNode.connect("depths", "depths", renderModelsNode);
        renderTransparentModelsNode.connect("colors", "colors", lightingNode);
        renderTransparentModelsNode.setFieldOfView(fov);
        renderTransparentModelsNode.setPlanes(planes);
        graph.addNode(renderTransparentModelsNode);
        // Render GUI
        renderGUINode = new RenderGUINode(graph, "gui");
        renderGUINode.connect("colors", "colors", renderTransparentModelsNode);
        renderGUINode.setPlanes(planes);
        graph.addNode(renderGUINode);
        // Build graph
        graph.rebuild();
    }

    private void initMaterials() {
        UniformHolder uniforms;
        // Solid material
        solidMaterial = new Material(graph.getProgram("solid"));
        uniforms = solidMaterial.getUniforms();
        uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
        uniforms.add(new FloatUniform("specularIntensity", 0.5f));
        uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
        uniforms.add(new FloatUniform("shininess", 0.15f));
        // Transparency
        transparencyMaterial = new Material(graph.getProgram("weightedSum"));
        uniforms = transparencyMaterial.getUniforms();
        uniforms.add(lightDirectionUniform);
        uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
        uniforms.add(new FloatUniform("specularIntensity", 1));
        uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
        uniforms.add(new FloatUniform("shininess", 0.8f));
    }

    private void addDefaultObjects() {
        addHUD();

        final VertexArray shape = context.newVertexArray();
        shape.create();
        shape.setData(MeshGenerator.generateCylinder(null, 2.5f, 5));
        final Model model1 = new Model(shape, transparencyMaterial);
        model1.setPosition(new Vector3f(0, 22, -6));
        model1.getUniforms().add(new Vector4Uniform("modelColor", new Vector4f(1, 1, 0, 0.3)));
        addTransparentModel(model1);
        final Model model2 = model1.getInstance();
        model2.setPosition(new Vector3f(0, 22, 6));
        model2.getUniforms().add(new Vector4Uniform("modelColor", new Vector4f(0, 1, 1, 0.7)));
        addTransparentModel(model2);
    }

    private void addHUD() {
        final Font ubuntu;
        try {
            ubuntu = Font.createFont(Font.TRUETYPE_FONT, FlowRenderer.class.getResourceAsStream("/fonts/ubuntu-r.ttf"));
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return;
        }
        final StringModel sandboxModel = new StringModel(context, graph.getProgram("font"), "FlowEngineFTPSInputPositionWRa0123456789-: ", ubuntu.deriveFont(Font.PLAIN, 15), windowSize.getX());
        final float aspect = getAspectRatio();
        sandboxModel.setPosition(new Vector3f(0.005, .97 * aspect, -0.1));
        sandboxModel.setString("Flow Engine - WIP");
        renderGUINode.addModel(sandboxModel);
        final StringModel fpsModel = sandboxModel.getInstance();
        final StringModel tpsModel = sandboxModel.getInstance();
        final StringModel itpsModel = sandboxModel.getInstance();
        fpsModel.setPosition(new Vector3f(0.005, .94 * aspect, -0.1));
        tpsModel.setPosition(new Vector3f(0.005, .91 * aspect, -0.1));
        itpsModel.setPosition(new Vector3f(0.005, .88 * aspect, -0.1));
        fpsModel.setString("FPS: " + fpsMonitor.getTPS());
        tpsModel.setString("TPS: " + scheduler.getMainThread().getTPS());
        itpsModel.setString("Input TPS: " + scheduler.getInputThread().getTPS());
        renderGUINode.addModel(fpsModel);
        renderGUINode.addModel(tpsModel);
        renderGUINode.addModel(itpsModel);
        fpsMonitorModel = fpsModel;
        tpsMonitorModel = tpsModel;
        itpsMonitorModel = itpsModel;
        final StringModel posModel = sandboxModel.getInstance();
        posModel.setPosition(new Vector3f(0.005, .85 * aspect, -0.1));
        posModel.setString("Position: " + renderModelsNode.getCamera().getPosition().toInt().toString() + " Rotation: " + renderModelsNode.getCamera().getRotation().toString());
        renderGUINode.addModel(posModel);
        positionModel = posModel;
    }

    /**
     * Destroys the renderer internal resources and the OpenGL context.
     */
    public void dispose() {
        disposeGraph();
        disposeContext();
        fpsMonitorStarted = false;
    }

    private void disposeContext() {
        context.destroy();
    }

    private void disposeGraph() {
        graph.destroy();
    }

    /**
     * Renders the models to the window.
     */
    public void render() {
        if (!fpsMonitorStarted) {
            fpsMonitor.start();
            fpsMonitorStarted = true;
        }
        // Update the current frame uniforms
        final Camera camera = renderModelsNode.getCamera();
        blurStrengthUniform.set((float) fpsMonitor.getTPS() / RenderThread.FPS);
        // Render
        graph.render();
        // Update the previous frame uniforms
        setPreviousModelMatrices();
        previousViewMatrixUniform.set(camera.getViewMatrix());
        previousProjectionMatrixUniform.set(camera.getProjectionMatrix());
        // Update the HUD
        updateHUD();
    }

    private void setPreviousModelMatrices() {
        for (Model model : renderModelsNode.getModels()) {
            model.getUniforms().getMatrix4("previousModelMatrix").set(model.getMatrix());
        }
    }

    private void updateHUD() {
        fpsMonitor.update();
        fpsMonitorModel.setString("FPS: " + fpsMonitor.getTPS());
        tpsMonitorModel.setString("TPS: " + scheduler.getMainThread().getTPS());
        itpsMonitorModel.setString("Input TPS: " + scheduler.getInputThread().getTPS());

        positionModel.setString("Position: " + renderModelsNode.getCamera().getPosition().toInt().toString() + " Rotation: " + renderModelsNode.getCamera().getRotation().toString());
    }

    /**
     * Returns the OpenGL version.
     *
     * @return The OpenGL version
     */
    public GLVersion getGLVersion() {
        return context.getGLVersion();
    }

    /**
     * Sets the OpenGL version. Must be done before initializing the renderer.
     *
     * @param version The OpenGL version to use
     */
    public void setGLVersion(GLVersion version) {
        switch (version) {
            case GL20:
                context = GLImplementation.get(LWJGLUtil.GL20_IMPL);
                break;
            case GL21:
                context = GLImplementation.get(LWJGLUtil.GL21_IMPL);
                break;
            case GL30:
            case GL31:
                context = GLImplementation.get(LWJGLUtil.GL30_IMPL);
                break;
            case GL32:
                context = GLImplementation.get(LWJGLUtil.GL32_IMPL);
                break;
            default:
                throw new IllegalArgumentException("Unsupported OpenGL version: " + version);
        }
    }

    public Context getContext() {
        return context;
    }

    public RenderModelsNode getRenderModelsNode() {
        return renderModelsNode;
    }

    public RenderGUINode getRenderGUINode() {
        return renderGUINode;
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
     * Updates the light direction and camera bounds to ensure that shadows are casted inside the frustum.
     *
     * @param direction The light direction
     * @param frustum The frustum in which to cast shadows
     */
    public void updateLight(Vector3f direction, ViewFrustum frustum) {
        direction = direction.normalize();
        lightDirectionUniform.set(direction);
        shadowMappingNode.updateLight(direction, frustum);
        lightingNode.setLightDirection(direction);
    }

    /**
     * Adds a model to be rendered as a solid.
     *
     * @param model The model
     */
    public void addSolidModel(Model model) {
        model.setMaterial(solidMaterial);
        model.getUniforms().add(new Vector4Uniform("modelColor", new Vector4f(Math.random(), Math.random(), Math.random(), 1)));
        renderModelsNode.addModel(model);
    }

    /**
     * Adds a model to be rendered as partially transparent.
     *
     * @param model The transparent model
     */
    public void addTransparentModel(Model model) {
        model.setMaterial(transparencyMaterial);
        renderTransparentModelsNode.addModel(model);
    }

    /**
     * Saves a screenshot (PNG) to the directory where the program is currently running, with the current date as the file name.
     *
     * @param outputDir The directory in which to output the file, can be null, which will output to the current working directory
     */
    public void saveScreenshot(File outputDir) {
        final Rectangle size = new Rectangle(Vector2i.ZERO, windowSize);
        final ByteBuffer buffer = context.readFrame(size, Texture.InternalFormat.RGB8);
        final BufferedImage image = CausticUtil.getImage(buffer, Format.RGB, size);
        try {
            ImageIO.write(image, "PNG", new File(outputDir, SCREENSHOT_DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Vector2i getResolution() {
        return windowSize;
    }

    @Override
    public float getAspectRatio() {
        return (float) windowSize.getY() / windowSize.getX();
    }
}