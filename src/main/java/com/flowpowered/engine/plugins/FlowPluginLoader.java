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

import java.lang.reflect.Field;
import java.util.Map;

import com.flowpowered.api.plugins.FlowPlugin;
import com.flowpowered.plugins.InvalidPluginException;
import com.flowpowered.plugins.PluginManager;
import com.flowpowered.plugins.simple.SimplePluginLoader;

public class FlowPluginLoader extends SimplePluginLoader {
    private static final Field engineField = getFieldSilent(FlowPlugin.class, "engine");

    public FlowPluginLoader(ClassLoader cl) {
        super(cl);
    }

    public FlowPluginLoader(ClassLoader cl, String descriptorName) {
        super(cl, descriptorName);
    }

    public FlowPluginLoader(ClassLoader cl, String descriptorName, String nameKey, String mainKey) {
        super(cl, descriptorName, nameKey, mainKey);
    }

    @Override
    public FlowPlugin load(PluginManager manager, String pluginName) throws InvalidPluginException {
        return load(manager, pluginName, findMains());
    }

    protected FlowPlugin load(PluginManager manager, String pluginName, Map<String, String> mains) throws InvalidPluginException {
        String main = mains.get(pluginName);
        if (main == null) {
            throw new InvalidPluginException("No main class specified");
        }
        try {
            Class<?> clazz = Class.forName(main, true, getClassLoader());
            Class<? extends FlowPlugin> pluginClass = clazz.asSubclass(FlowPlugin.class);
            return init(pluginClass.newInstance(), pluginName, manager);
        } catch (ClassNotFoundException | ClassCastException | InstantiationException | IllegalAccessException e) {
            // TODO: log
            e.printStackTrace();
        } catch (ExceptionInInitializerError e) {
            throw new InvalidPluginException("Exception in Plugin initialization", e);
        }
        return null;
    }

    protected FlowPlugin init(FlowPlugin plugin, String name, PluginManager manager) throws IllegalArgumentException, IllegalAccessException {
        plugin = (FlowPlugin) super.init(plugin, name, manager);
        setField(engineField, plugin, ((FlowPluginManager) manager).getEngine());
        return plugin;
    }
}