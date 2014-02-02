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
package com.flowpowered.api.component.block;

import com.flowpowered.api.component.BlockComponentOwner;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.component.ComponentOwner;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.geo.discrete.Point;

public abstract class BlockComponent extends Component {
    @Override
    public final boolean attachTo(ComponentOwner owner) {
        if (!(owner instanceof BlockComponentOwner)) {
            throw new IllegalStateException("BlockComponents may only be attached to a BlockComponentOwner.");
        }
        return super.attachTo(owner);
    }

    @Override
    public final BlockComponentOwner getOwner() {
        return (BlockComponentOwner) super.getOwner();
    }

    /**
     * Gets the position of this block component
     *
     * @return position
     */
    public Point getPoint() {
        final BlockComponentOwner owner = getOwner();
        if (owner == null) {
            throw new IllegalStateException("This component has no owner and therefore no point");
        }
        return new Point(owner.getWorld(), owner.getX(), owner.getY(), owner.getZ());
    }

    /**
     * Gets the {@link Block} who owns this component.
     *
     * The structure of BlockComponents differ from the other {@link ComponentOwner}s. {@link BlockComponentOwner} is what does BlockComponent management but Block itself owns the block. To keep things
     * easy to access, this convenience method is provided.
     *
     * @return the block associated with the BlockComponentOwner
     */
    public Block getBlock() {
        final BlockComponentOwner owner = getOwner();
        if (owner == null) {
            throw new IllegalStateException("This component has no owner and therefore no block");
        }
        return owner.getBlock();
    }

    /**
     * Called when the owning {@link com.flowpowered.api.geo.cuboid.Block} is collided with an {@link Entity}.
     *
     * @param point the point where collision occurred.
     * @param entity the entity that collided with the owner <p> TODO EntityCollideBlockEvent
     */
    public void onCollided(Point point, Entity entity) {
    }
}
