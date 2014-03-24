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


import com.flowpowered.engine.render.graph.RenderGraph;
import org.spout.renderer.api.gl.Texture;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public abstract class GraphNode {
    protected final RenderGraph graph;
    protected final String name;
    private final Map<String, Method> inputs = new HashMap<>();
    private final Map<String, Method> outputs = new HashMap<>();
    private final Map<String, GraphNode> inputNodes = new HashMap<>();
    private final Map<String, GraphNode> outputNodes = new HashMap<>();

    protected GraphNode(RenderGraph graph, String name) {
        this.graph = graph;
        this.name = name;
        findInputsAndOutputs();
    }

    public abstract void render();

    public abstract void destroy();

    public String getName() {
        return name;
    }

    public Set<String> getInputs() {
        return Collections.unmodifiableSet(inputs.keySet());
    }

    public Set<String> getOutputs() {
        return Collections.unmodifiableSet(outputs.keySet());
    }

    public Map<String, GraphNode> getConnectedInputs() {
        return Collections.unmodifiableMap(inputNodes);
    }

    public Map<String, GraphNode> getConnectedOutputs() {
        return Collections.unmodifiableMap(outputNodes);
    }

    public void connect(String input, String output, GraphNode parent) {
        setInput(input, parent.getOutput(output));
        inputNodes.put(input, parent);
        parent.outputNodes.put(output, this);
    }

    private void setInput(String name, Object input) {
        try {
            inputs.get(name).invoke(this, input);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to set node input", ex);
        }
    }

    private Object getOutput(String name) {
        try {
            return outputs.get(name).invoke(this);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get node output", ex);
        }
    }

    private void findInputsAndOutputs() {
        for (Method method : getClass().getMethods()) {
            method.setAccessible(true);
            final Input inputAnnotation = method.getAnnotation(Input.class);
            final Output outputAnnotation = method.getAnnotation(Output.class);
            if (inputAnnotation != null) {
                if (outputAnnotation != null) {
                    throw new IllegalStateException("Input and output annotations cannot be both present");
                }
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1 || !Texture.class.isAssignableFrom(parameterTypes[0])) {
                    throw new IllegalStateException("Output method must have one argument of type " + Texture.class.getCanonicalName());
                }
                inputs.put(inputAnnotation.value(), method);
            } else if (outputAnnotation != null) {
                if (!Texture.class.isAssignableFrom(method.getReturnType())) {
                    throw new IllegalStateException("Input method must have return type " + Texture.class.getCanonicalName());
                }
                outputs.put(outputAnnotation.value(), method);
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public static @interface Input {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public static @interface Output {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public static @interface Setting {
    }
}