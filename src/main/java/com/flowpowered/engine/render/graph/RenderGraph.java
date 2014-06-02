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

import org.spout.renderer.api.Creatable;
import org.spout.renderer.api.data.ShaderSource;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Shader;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.util.MeshGenerator;

import com.flowpowered.engine.render.FlowRenderer;
import com.flowpowered.engine.render.graph.node.GraphNode;
import com.flowpowered.math.vector.Vector2f;

/**
 *
 */
public class RenderGraph extends Creatable {
    private final Context context;
    private final String shaderSrcDir;
    private final Map<String, Program> programs = new HashMap<>();
    private final VertexArray screen;
    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final SortedSet<Stage> stages = new TreeSet<>();

    public RenderGraph(Context context, String shaderSrcDir) {
        this.context = context;
        this.shaderSrcDir = shaderSrcDir;
        screen = context.newVertexArray();
    }

    @Override
    public void create() {
        if (isCreated()) {
            throw new IllegalStateException("Render graph has already been created");
        }
        // Create the full screen quad
        screen.create();
        screen.setData(MeshGenerator.generatePlane(new Vector2f(2, 2)));
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
        for (Program program : programs.values()) {
            for (Shader shader : program.getShaders()) {
                shader.destroy();
            }
            program.destroy();
        }
        programs.clear();
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
    }

    public void render() {
        for (Stage stage : stages) {
            stage.render();
        }
        context.updateDisplay();
    }

    public void addNode(GraphNode node) {
        nodes.put(node.getName(), node);
    }

    public GraphNode getNode(String name) {
        return nodes.get(name);
    }

    public Context getContext() {
        return context;
    }

    public VertexArray getScreen() {
        return screen;
    }

    public Program getProgram(String name) {
        final Program program = programs.get(name);
        if (program == null) {
            return loadProgram(name);
        }
        return program;
    }

    private Program loadProgram(String name) {
        final String shaderPath = shaderSrcDir + '/' + name;
        final Shader vertex = context.newShader();
        vertex.create();
        vertex.setSource(new ShaderSource(FlowRenderer.class.getResourceAsStream(shaderPath + ".vert")));
        vertex.compile();
        final Shader fragment = context.newShader();
        fragment.create();
        fragment.setSource(new ShaderSource(FlowRenderer.class.getResourceAsStream(shaderPath + ".frag")));
        fragment.compile();
        final Program program = context.newProgram();
        program.create();
        program.attachShader(vertex);
        program.attachShader(fragment);
        program.link();
        programs.put(name, program);
        return program;
    }

    private static class Stage implements Comparable<Stage> {
        private final Set<GraphNode> nodes = new HashSet<>();
        private final int number;

        private Stage(int number) {
            this.number = number;
        }

        private void addNode(GraphNode node) {
            nodes.add(node);
        }

        public Set<GraphNode> getNodes() {
            return nodes;
        }

        private void render() {
            for (GraphNode node : nodes) {
                node.render();
            }
        }

        private int getNumber() {
            return number;
        }

        @Override
        public int compareTo(Stage o) {
            return number - o.getNumber();
        }
    }
}