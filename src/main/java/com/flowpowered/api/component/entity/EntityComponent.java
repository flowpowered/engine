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
package com.flowpowered.api.component.entity;

import java.util.Random;

import com.flowpowered.api.Engine;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.component.ComponentOwner;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.math.GenericMath;

/**
 * Represents a component who shapes the logic behind an {@link Entity}.
 */
public abstract class EntityComponent extends Component {
    @Override
    public final boolean attachTo(ComponentOwner owner) {
        if (!(owner instanceof Entity)) {
            throw new IllegalStateException("EntityComponents may only be attached to Entities.");
        }
        return super.attachTo(owner);
    }

    @Override
    public Entity getOwner() {
        return (Entity) super.getOwner();
    }

    /**
     * Returns a deterministic random number generator
     *
     * @return random the random generator
     */
    public final Random getRandom() {
        return GenericMath.getRandom();
    }

    public final Engine getEngine() {
        final Entity owner = getOwner();
        if (owner == null) {
            throw new IllegalStateException("Can not access the engine w/o an owner");
        }
        return owner.getEngine();
    }

    /**
     * Called when the owner comes within range of another owner with an attached {@link ObserverComponent}. <p> TODO EntityObservedEvent
     */
    public void onObserved() {
    }

    /**
     * Called when the owner is out of range of any owners with attached {@link ObserverComponent}s. <p> TODO EntityUnObservedEvent
     */
    public void onUnObserved() {
    }
}
