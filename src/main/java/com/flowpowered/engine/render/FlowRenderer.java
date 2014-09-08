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
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;

import com.flowpowered.api.render.Renderer;
import com.flowpowered.commons.TPSMonitor;
import com.flowpowered.engine.geo.region.RegionGenerator;
import com.flowpowered.engine.scheduler.FlowScheduler;
import com.flowpowered.engine.scheduler.render.RenderThread;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;
import com.flowpowered.render.RenderGraph;
import com.flowpowered.render.impl.BlurNode;
import com.flowpowered.render.impl.CascadedShadowMappingNode;
import com.flowpowered.render.impl.LightingNode;
import com.flowpowered.render.impl.RenderGUINode;
import com.flowpowered.render.impl.RenderModelsNode;
import com.flowpowered.render.impl.RenderTransparentModelsNode;
import com.flowpowered.render.impl.SSAONode;
import com.flowpowered.render.impl.ShadowMappingNode;

import org.lwjgl.opengl.GLContext;

import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLImplementation;
import org.spout.renderer.api.GLVersioned.GLVersion;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.Uniform.Vector4Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.Context.Capability;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.model.StringModel;
import org.spout.renderer.api.util.CausticUtil;
import org.spout.renderer.api.util.MeshGenerator;
import org.spout.renderer.api.util.Rectangle;
import org.spout.renderer.lwjgl.LWJGLUtil;

public class FlowRenderer implements Renderer {
    private static final String WINDOW_TITLE = "Flow Engine";
    private static final DateFormat SCREENSHOT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    // Settings
    private Vector2i windowSize = new Vector2i(1200, 800);
    private boolean cullBackFaces = true;
    // Effect uniforms
    private final Vector3Uniform lightDirectionUniform = new Vector3Uniform("lightDirection", Vector3f.FORWARD);
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
    // Models
    private final List<Model> models = new ArrayList<>();
    private final List<Model> guiModels = new ArrayList<>();
    private final List<Model> transparentModels = new ArrayList<>();
    // FPS, TPS, and position monitor and models
    private final TPSMonitor fpsMonitor = new TPSMonitor();
    private StringModel fpsMonitorModel;
    private StringModel tpsMonitorModel;
    private StringModel itpsMonitorModel;
    private StringModel positionModel;
    private StringModel genCountModel;
    private boolean fpsMonitorStarted = false;

    private FlowScheduler scheduler;

    public FlowRenderer() {
        // Set the default OpenGL version to GL30
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
    }

    private void initGraph() {
        final int blurSize = 2;
        // Create the graph
        graph = new RenderGraph(context, "/shaders/glsl" + (context.getGLVersion().getMajor() == 2 ? 120 : 330));
        graph.create();
        graph.setAttribute("camera", Camera.createPerspective(60, windowSize.getX(), windowSize.getY(), 0.1f, 200));
        graph.setAttribute("outputSize", windowSize);
        graph.setAttribute("lightDirection", Vector3f.UP.negate());
        graph.setAttribute("models", models);
        // Render models
        renderModelsNode = new RenderModelsNode(graph, "models");
        graph.addNode(renderModelsNode);
        // Shadows
        shadowMappingNode = new CascadedShadowMappingNode(graph, "shadows");
        shadowMappingNode.connect("normals", "vertexNormals", renderModelsNode);
        shadowMappingNode.connect("depths", "depths", renderModelsNode);
        shadowMappingNode.setAttribute("shadowMapSize", new Vector2i(1048, 1048));
        shadowMappingNode.setAttribute("renderModelsNode", renderModelsNode);
        shadowMappingNode.setAttribute("kernelSize", 8);
        shadowMappingNode.setAttribute("noiseSize", blurSize);
        shadowMappingNode.setAttribute("bias", 0.01f);
        shadowMappingNode.setAttribute("radius", 0.05f);
        graph.addNode(shadowMappingNode);
        // Blur shadows
        final BlurNode blurShadowsNode = new BlurNode(graph, "blurShadows");
        blurShadowsNode.connect("colors", "shadows", shadowMappingNode);
        blurShadowsNode.setAttribute("kernelGenerator", BlurNode.BOX_KERNEL);
        blurShadowsNode.setAttribute("kernelSize", blurSize + 1);
        graph.addNode(blurShadowsNode);
        // SSAO
        final SSAONode ssaoNode = new SSAONode(graph, "ssao");
        ssaoNode.connect("normals", "normals", renderModelsNode);
        ssaoNode.connect("depths", "depths", renderModelsNode);
        ssaoNode.setAttribute("kernelSize", 8);
        ssaoNode.setAttribute("threshold", 0.15f);
        ssaoNode.setAttribute("noiseSize", blurSize);
        ssaoNode.setAttribute("radius", 0.5f);
        ssaoNode.setAttribute("power", 2f);
        graph.addNode(ssaoNode);
        // Blur occlusions
        final BlurNode blurOcclusionsNode = new BlurNode(graph, "blurOcclusions");
        blurOcclusionsNode.connect("colors", "occlusions", ssaoNode);
        blurOcclusionsNode.setAttribute("kernelGenerator", BlurNode.BOX_KERNEL);
        blurOcclusionsNode.setAttribute("kernelSize", blurSize + 1);
        blurOcclusionsNode.setAttribute("outputFormat", InternalFormat.R8);
        graph.addNode(blurOcclusionsNode);
        // Lighting
        lightingNode = new LightingNode(graph, "lighting");
        lightingNode.connect("colors", "colors", renderModelsNode);
        lightingNode.connect("normals", "normals", renderModelsNode);
        lightingNode.connect("depths", "depths", renderModelsNode);
        lightingNode.connect("materials", "materials", renderModelsNode);
        lightingNode.connect("occlusions", "colors", blurOcclusionsNode);
        lightingNode.connect("shadows", "colors", blurShadowsNode);
        graph.addNode(lightingNode);
        // Transparent models
        renderTransparentModelsNode = new RenderTransparentModelsNode(graph, "transparency");
        renderTransparentModelsNode.connect("depths", "depths", renderModelsNode);
        renderTransparentModelsNode.connect("colors", "colors", lightingNode);
        renderTransparentModelsNode.setAttribute("transparentModels", transparentModels);
        graph.addNode(renderTransparentModelsNode);
        // Render GUI
        renderGUINode = new RenderGUINode(graph, "gui");
        renderGUINode.connect("colors", "colors", renderTransparentModelsNode);
        renderGUINode.setAttribute("guiModels", guiModels);
        graph.addNode(renderGUINode);

        graph.updateAll();

        // Build graph
        graph.build();
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
        // Transparency material
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
        shape.setData(MeshGenerator.generateCylinder(2.5f, 5));
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
        final StringModel sandboxModel = new StringModel(context, graph.getProgram("font"), "FlowEngineFTPSInputPositionWRa0123456789.-: GenCount", ubuntu.deriveFont(Font.PLAIN, 15), windowSize.getX());
        final float aspect = getAspectRatio();
        sandboxModel.setPosition(new Vector3f(0.005, .97 * aspect, -0.1));
        sandboxModel.setString("Flow Engine - WIP");
        guiModels.add(sandboxModel);

        final StringModel fpsModel = sandboxModel.getInstance();
        final StringModel tpsModel = sandboxModel.getInstance();
        final StringModel itpsModel = sandboxModel.getInstance();
        fpsModel.setPosition(new Vector3f(0.005, .94 * aspect, -0.1));
        tpsModel.setPosition(new Vector3f(0.005, .91 * aspect, -0.1));
        itpsModel.setPosition(new Vector3f(0.005, .88 * aspect, -0.1));
        guiModels.add(fpsModel);
        guiModels.add(tpsModel);
        guiModels.add(itpsModel);
        fpsMonitorModel = fpsModel;
        tpsMonitorModel = tpsModel;
        itpsMonitorModel = itpsModel;

        final StringModel posModel = sandboxModel.getInstance();
        posModel.setPosition(new Vector3f(0.005, .85 * aspect, -0.1));
        guiModels.add(posModel);
        positionModel = posModel;

        final StringModel genCoModel = sandboxModel.getInstance();
        genCoModel.setPosition(new Vector3f(0.005, .82 * aspect, -0.1));
        guiModels.add(genCoModel);
        genCountModel = genCoModel;

        updateHUD();
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
        // Render
        graph.render();
        // Update the HUD
        updateHUD();
    }

    private void updateHUD() {
        fpsMonitor.update();
        fpsMonitorModel.setString("FPS: " + fpsMonitor.getTPS());
        tpsMonitorModel.setString("TPS: " + scheduler.getMainThread().getTPS());
        itpsMonitorModel.setString("Input TPS: " + scheduler.getInputThread().getTPS());

        Camera camera = renderModelsNode.getAttribute("camera");
        positionModel.setString("Position: " + camera.getPosition().toInt().toString() + " Rotation: " + camera.getRotation().toString());

        genCountModel.setString("GenCount: " + RegionGenerator.getGenCount());
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
    public void updateLight(Vector3f direction) {
        direction = direction.normalize();
        lightDirectionUniform.set(direction);
        graph.setAttribute("lightDirection", direction);
    }

    /**
     * Adds a model to be rendered as a solid.
     *
     * @param model The model
     */
    public void addSolidModel(Model model) {
        model.setMaterial(solidMaterial);
        model.getUniforms().add(new Vector4Uniform("modelColor", new Vector4f(Math.random(), Math.random(), Math.random(), 1)));
        models.add(model);
    }

    /**
     * Adds a model to be rendered as partially transparent.
     *
     * @param model The transparent model
     */
    public void addTransparentModel(Model model) {
        model.setMaterial(transparencyMaterial);
        transparentModels.add(model);
    }

    public void removeModel(Model model) {
        models.remove(model);
    }

    public void removeTransparentModels(Model model) {
        transparentModels.remove(model);
    }

    /**
     * Saves a screenshot (PNG) to the directory where the program is currently running, with the current date as the file name.
     *
     * @param outputDir The directory in which to output the file, can be null, which will output to the current working directory
     */
    public void saveScreenshot(File outputDir) {
        final Rectangle size = new Rectangle(Vector2i.ZERO, windowSize);
        final ByteBuffer buffer = context.readFrame(size, InternalFormat.RGB8);
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
