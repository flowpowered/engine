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
package com.flowpowered.engine.plugins;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.flowpowered.api.plugins.FlowContext;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.filesystem.FlowFileSystem;
import com.flowpowered.plugins.ContextCreator;
import com.flowpowered.plugins.Plugin;
import com.flowpowered.plugins.PluginManager;
import com.flowpowered.plugins.simple.SimplePluginLoader;

import org.slf4j.Logger;

public class FlowPluginManager extends PluginManager<FlowContext> {
    private final FlowEngine engine;

    public FlowPluginManager(Logger logger, final FlowEngine engine) {
        super(logger);

        try {
            Files.createDirectories(FlowFileSystem.PLUGINS_DIRECTORY);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.engine = engine;
        ContextCreator<FlowContext> cc = new ContextCreator<FlowContext>() {
            @Override
            public FlowContext createContext(Plugin<FlowContext> plugin) {
                return new FlowContext(plugin, engine);
            }
        };
        addLoader(new SimplePluginLoader<>(cc, new URLClassLoader(getURLs(FlowFileSystem.PLUGINS_DIRECTORY, "*.jar"))));
    }

    public void enablePlugins() {
        for(Plugin<FlowContext> p : getPlugins()) {
            try {
                p.enable();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void disablePlugins() {
        for(Plugin<FlowContext> p : getPlugins()) {
            try {
                p.disable();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private URL[] getURLs(Path path, String blob) {
        List<URL> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, blob)) {
            for (Path entry: stream) {
                result.add(entry.toUri().toURL());
            }
        } catch (IOException ex) {
            // I/O error encounted during the iteration, the cause is an IOException
            ex.printStackTrace();
        }

        URL[] l = new URL[result.size()];
        return result.toArray(l);
    }

    public FlowEngine getEngine() {
        return engine;
    }
}
