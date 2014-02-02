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

import java.util.Collection;

import com.flowpowered.commons.datatable.ManagedMap;

/**
 * Represents an object which may own components.
 */
public interface ComponentOwner {
    /**
     * Adds the component of the specified type to the owner and returns it if it is not present. <p> Otherwise, it returns the component of the specified type if there was one present. </p>
     *
     * @param type whose component is to be added to the owner
     * @return the new component that was added, or the existing one if it had one
     */
    public <T extends Component> T add(Class<T> type);

    /**
     * Returns the component of the specified type (or a child implementation) from the owner if it is present.
     *
     * @param type whose component is to be returned from the holder
     * @return the component, or null if one was not found
     */
    public <T extends Component> T get(Class<T> type);

    /**
     * Returns all components of the specified type (or a child implementation).
     *
     * @param type whose components are to be returned from the owner
     * @return the component list.
     */
    public <T extends Component> Collection<T> getAll(Class<T> type);

    /**
     * Returns all instances of the specified type from the owner if they are present.
     *
     * @param type whose components are to be returned from the owner
     * @return the component list.
     */
    public <T> Collection<T> getAllOfType(Class<T> type);

    /**
     * Returns the component of the specified type (not a child implementation) from the holder if it is present.
     *
     * @param type whose component is to be returned from the owner
     * @return the component, or null if one was not found.
     */
    public <T extends Component> T getExact(Class<T> type);

    /**
     * Returns an instance of the specified type from the owner if it is present.
     *
     * @param type whose component is to be returned from the owner
     * @return the component, or null if one was not found
     */
    public <T> T getType(Class<T> type);

    /**
     * Removes the component of the specified type from the owner if it is present.
     *
     * @param type whose component is to be removed from the owner
     * @return the removed component, or null if there was not one
     */
    public <T extends Component> T detach(Class<? extends Component> type);

    /**
     * Gets all components held by the owner.
     *
     * @return A collection of held components
     */
    public Collection<Component> values();

    /**
     * Gets the {@link ManagedMap} of the owner.
     *
     * @return datatable component
     */
    public ManagedMap getData();
}
