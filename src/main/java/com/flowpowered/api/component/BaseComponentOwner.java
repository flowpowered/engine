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
package com.flowpowered.api.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import com.flowpowered.api.Engine;
import com.flowpowered.commons.datatable.ManagedHashMap;
import com.flowpowered.events.Listener;

public class BaseComponentOwner implements ComponentOwner {
    /**
     * Map of class name, component
     */
    private final BiMap<Class<? extends Component>, Component> components = HashBiMap.create();
    private final ManagedHashMap data;
    private final Engine engine;

    public BaseComponentOwner(Engine engine) {
        this(engine, new ManagedHashMap());
    }

    public BaseComponentOwner(Engine engine, ManagedHashMap data) {
        this.engine = engine;
        this.data = data;
    }

    /**
     * For use de-serializing a list of components all at once, without having to worry about dependencies
     */
    @SuppressWarnings("unchecked")
    protected void add(Class<? extends Component>... components) {
        HashSet<Component> added = new HashSet<>();
        synchronized (components) {
            for (Class<? extends Component> type : components) {
                if (!this.components.containsKey(type)) {
                    added.add(add(type, false));
                }
            }
        }
        for (Component type : added) {
            type.onAttached();
        }
    }

    @Override
    public <T extends Component> T add(Class<T> type) {
        return add(type, true);
    }

    /**
     * Adds a component to the map
     *
     * @param type to add
     * @param attach whether to call the component onAttached
     * @return instantiated component
     */
    protected <T extends Component> T add(Class<T> type, boolean attach) {
        return add(type, type, attach);
    }

    /**
     * Adds a component to the map
     *
     * @param key the component class used as the lookup key
     * @param type of component to instantiate
     * @param attach whether to call the component onAttached
     * @return instantiated component
     */
    @SuppressWarnings ("unchecked")
    protected final <T extends Component> T add(Class<T> key, Class<? extends Component> type, boolean attach) {
        if (type == null || key == null) {
            return null;
        }

        synchronized (components) {
            T component = (T) components.get(key);

            if (component != null) {
                return component;
            }

            try {
                component = (T) type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

            if (component != null) {
                try {
                    attachComponent(key, component, attach);
                } catch (RuntimeException e) {
                    engine.getLogger().error("Error while attaching component " + type + ": ", e);
                }
            }
            return component;
        }
    }

    protected void attachComponent(Class<? extends Component> key, Component component, boolean attach) {
        if (component.attachTo(this)) {
            components.put(key, component);
            if (attach) {
                try {
                    component.onAttached();
                } catch (RuntimeException e) {
                    // Remove the component from the component map if onAttached can't be
                    // called, pass exception to next catch block.
                    components.remove(key);
                    throw e;
                }
            }
        }
    }

    @Override
    public <T extends Component> T detach(Class<? extends Component> type) {
        return detach(type, false);
    }

    @SuppressWarnings ("unchecked")
    protected <T extends Component> T detach(Class<? extends Component> type, boolean force) {
        Preconditions.checkNotNull(type);
        synchronized (components) {
            T component = (T) get(type);

            if (component != null && (component.isDetachable() || force)) {
                components.inverse().remove(component);
                try {
                    component.onDetached();
                } catch (Exception e) {
                    engine.getLogger().error("Error detaching component " + type + " from holder: ", e);
                }
                if (component instanceof Listener) {
                    engine.getEventManager().unRegisterEvents((Listener) component);
                }
            }

            return component;
        }
    }

    @SuppressWarnings ("unchecked")
    @Override
    public <T extends Component> T get(Class<T> type) {
        Preconditions.checkNotNull(type);
        Component component = components.get(type);

        if (component == null) {
            component = findComponent(type);
        }
        return (T) component;
    }

    @Override
    public <T> T getType(Class<T> type) {
        Preconditions.checkNotNull(type);

        T component = findComponent(type);

        return component;
    }

    @SuppressWarnings ("unchecked")
    @Override
    public <T extends Component> T getExact(Class<T> type) {
        Preconditions.checkNotNull(type);
        synchronized (components) {
            return (T) components.get(type);
        }
    }

    @SuppressWarnings ("unchecked")
    @Override
    public <T extends Component> Collection<T> getAll(Class<T> type) {
        Preconditions.checkNotNull(type);
        synchronized (components) {
            ArrayList<T> matches = new ArrayList<>();
            for (Component component : components.values()) {
                if (type.isAssignableFrom(component.getClass())) {
                    matches.add((T) component);
                }
            }
            return matches;
        }
    }

    @SuppressWarnings ("unchecked")
    @Override
    public <T extends Object> Collection<T> getAllOfType(Class<T> type) {
        Preconditions.checkNotNull(type);
        synchronized (components) {
            ArrayList<T> matches = new ArrayList<>();
            for (Component component : components.values()) {
                if (type.isAssignableFrom(component.getClass())) {
                    matches.add((T) component);
                }
            }
            return matches;
        }
    }

    @Override
    public Collection<Component> values() {
        synchronized (components) {
            return new ArrayList<>(components.values());
        }
    }

    @Override
    public ManagedHashMap getData() {
        return data;
    }

    @SuppressWarnings ("unchecked")
    private <T> T findComponent(Class<T> type) {
        Preconditions.checkNotNull(type);
        synchronized (components) {
            for (Component component : values()) {
                if (type.isAssignableFrom(component.getClass())) {
                    return (T) component;
                }
            }
        }

        return null;
    }

    public Engine getEngine() {
        return engine;
    }
}
