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
package com.flowpowered.engine.render.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.flowpowered.math.vector.Vector2f;

import org.spout.renderer.api.Creatable;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.GLFactory;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Shader;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.util.MeshGenerator;

import com.flowpowered.engine.render.FlowRenderer;
import com.flowpowered.engine.render.graph.node.GraphNode;

/**
 *
 */
public class RenderGraph extends Creatable {
    private final GLFactory glFactory;
    private final Context glContext;
    private final ProgramPool programPool;
    private final TexturePool texturePool = new TexturePool();
    private final VertexArray screen;
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final SortedSet<Stage> stages = new TreeSet<>();

    public RenderGraph(GLFactory glFactory, Context glContext, String shaderSrcDir) {
        this.glFactory = glFactory;
        this.glContext = glContext;
        programPool = new ProgramPool(shaderSrcDir);
        screen = glFactory.createVertexArray();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Render graph has already been created");
        }
        // Create the full screen quad
        screen.setData(MeshGenerator.generateTexturedPlane(null, new Vector2f(2, 2)));
        screen.create();
        // Update the state to created
        super.create();
    }

    @Override
    public void destroy() {
        checkCreated();
        screen.destroy();
        for (GraphNode node : nodes.values()) {
            node.destroy();
        }
        nodes.clear();
        stages.clear();
        programPool.dispose();
        texturePool.dispose();
        super.destroy();
    }

    public void rebuild() {
        stages.clear();
        final Set<GraphNode> toBuild = new HashSet<>(nodes.values());
        final Set<GraphNode> previous = new HashSet<>();
        int i = 0;
        Stage current = new Stage(i++);
        while (true) {
            for (Iterator<GraphNode> iterator = toBuild.iterator(); iterator.hasNext(); ) {
                final GraphNode node = iterator.next();
                if (previous.containsAll(node.getConnectedInputs().values())) {
                    current.addNode(node);
                    iterator.remove();
                }
            }
            if (current.getNodes().isEmpty()) {
                return;
            }
            previous.addAll(current.getNodes());
            stages.add(current);
            if (toBuild.isEmpty()) {
                break;
            }
            current = new Stage(i++);
        }
        for (Stage stage : stages) {
            System.out.println(stage.getNumber() + ": " + stage.getNodes());
        }
}

    public void render() {
        for (Stage stage : stages) {
            stage.render();
        }
    }

    public void addNode(GraphNode node) {
        nodes.put(node.getName(), node);
    }

    public GraphNode getNode(String name) {
        return nodes.get(name);
    }

    public GLFactory getGLFactory() {
        return glFactory;
    }

    public Context getContext() {
        return glContext;
    }

    public VertexArray getScreen() {
       return screen;
    }

    public Program getProgram(String name) {
        return programPool.get(name);
    }

    private class ProgramPool {
        private final String sourceDirectory;
        private final Map<String, Program> programs = new HashMap<>();

        public ProgramPool(String sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
        }

        public Program get(String name) {
            final Program program = programs.get(name);
            if (program == null) {
                return loadProgram(name);
            }
            return program;
        }

        public void dispose() {
            for (Program program : programs.values()) {
                for (Shader shader : program.getShaders()) {
                   shader.destroy();
                }
                program.destroy();
            }
        }

        private Program loadProgram(String name) {
            final String shaderPath = sourceDirectory + "/" + name;
            System.out.println(shaderPath);
            final Shader vertex = glFactory.createShader();
            vertex.setSource(FlowRenderer.class.getResourceAsStream(shaderPath + ".vert"));
            vertex.create();
            final Shader fragment = glFactory.createShader();
            fragment.setSource(FlowRenderer.class.getResourceAsStream(shaderPath + ".frag"));
            fragment.create();
            final Program program = glFactory.createProgram();
            program.addShader(vertex);
            program.addShader(fragment);
            program.create();
            programs.put(name, program);
            return program;
        }
    }

    private class TexturePool {
        private final Set<Texture> textures = new HashSet<>();

        private Texture get(int width, int height, InternalFormat format) {
            return null;
        }

        private void dispose() {

        }

        private float checkMatch(InternalFormat desired, InternalFormat candidate) {
            if (candidate.getComponentCount() < desired.getComponentCount()
                    || desired.hasRed() && !candidate.hasRed()
                    || desired.hasGreen() && !candidate.hasGreen()
                    || desired.hasBlue() && !candidate.hasBlue()
                    || desired.hasAlpha() && !candidate.hasAlpha()
                    || desired.hasDepth() && !candidate.hasDepth()
                    || desired.isFloatBased() && !candidate.isFloatBased()) {
                return -1;
            }
            float match = 0;

            if (candidate.hasRed() && !desired.hasRed()) { match++; }

            if (candidate.hasGreen() && !desired.hasGreen()) { match++; }

            if (candidate.hasBlue() && !desired.hasBlue()) {
                match++;
            }
            if (candidate.hasAlpha() && !desired.hasAlpha()) {
                match++;
            }
            if (candidate.hasDepth() && !desired.hasDepth()) {
                 match++;
            }
              if (candidate.isFloatBased() && !desired.isFloatBased()) {
                 match++;
            }
            final float byteRatio = candidate.getBytesPerComponent() / (float) desired.getBytesPerComponent();
            if (byteRatio < 1) {
                return -1;
            } else {
                return match + byteRatio - 1;
            }
        }
    }

    private static class Stage implements Comparable<Stage> {
        private final Set<GraphNode> nodes = new HashSet<>();
        private final int number;

        private Stage(int number) { this.number = number; }

        private void addNode(GraphNode node) { nodes.add(node); }

       public Set<GraphNode> getNodes() { return nodes; }

        private void render() {
            for (GraphNode node : nodes) { node.render(); }
        }

        private int getNumber() { return number; }

        @Override
        public int compareTo(Stage o) { return number - o.getNumber(); }
    }
}