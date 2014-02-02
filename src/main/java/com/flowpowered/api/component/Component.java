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

import com.flowpowered.api.scheduler.tickable.BasicTickable;
import com.flowpowered.commons.datatable.SerializableMap;

public abstract class Component extends BasicTickable {
    private ComponentOwner owner;

    /**
     * Attaches to a component owner.
     *
     * @param owner the component owner to attach to
     * @return true if successful
     */
    public boolean attachTo(ComponentOwner owner) {
        this.owner = owner;
        return true;
    }

    /**
     * Gets the component owner that owns this component.
     *
     * @return the component owner
     */
    public ComponentOwner getOwner() {
        if (owner == null) {
            throw new IllegalStateException("Trying to access the owner of this component before it was attached");
        }
        return owner;
    }

    /**
     * Called when this component is attached to a owner.
     */
    public void onAttached() {
    }

    /**
     * Called when this component is detached from a owner.
     */
    public void onDetached() {
    }

    /**
     * Specifies whether or not this component can be detached, after it has already been attached to an owner..
     *
     * @return true if it can be detached
     */
    public boolean isDetachable() {
        return true;
    }

    /**
     * Gets the {@link SerializableMap} which a ComponentOwner always has <p> This is merely a convenience method.
     *
     * @return SerializableMap of the owner
     */
    public final SerializableMap getData() {
        return getOwner().getData();
    }
}
