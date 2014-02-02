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
package com.flowpowered.engine.geo;

import com.flowpowered.engine.geo.chunk.FlowChunk;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.geo.region.FlowRegion;
import java.lang.ref.WeakReference;
import java.util.Collection;

import com.flowpowered.commons.StringUtil;
import com.flowpowered.commons.datatable.ManagedMap;
import com.flowpowered.events.Cause;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.flowpowered.api.Platform;
import com.flowpowered.api.Flow;
import com.flowpowered.api.component.BlockComponentOwner;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.geo.cuboid.reference.ChunkReference;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.material.Material;
import com.flowpowered.api.material.block.BlockFace;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;

public class FlowBlock implements Block {
    private final int x, y, z;
    private final WeakReference<? extends FlowWorld> world;
    private final ChunkReference chunk;

    public FlowBlock(FlowWorld world, int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = new WeakReference<>(world);
        this.chunk = new ChunkReference(new Point(getWorld(), this.x, this.y, this.z));
    }

    @Override
    public Point getPosition() {
        return new Point(getWorld(), this.x + 0.5f, this.y + 0.5f, this.z + 0.5f);
    }

    @Override
    public FlowChunk getChunk() {
        return (FlowChunk) this.chunk.refresh(LoadOption.LOAD_GEN);
    }

    @Override
    public FlowWorld getWorld() {
        FlowWorld world = this.world.get();
        if (world == null) {
            throw new IllegalStateException("The world has been unloaded!");
        }
        return world;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getZ() {
        return this.z;
    }

    @Override
    public Block translate(BlockFace offset, int distance) {
        return this.translate(offset.getOffset().mul(distance));
    }

    @Override
    public Block translate(BlockFace offset) {
        if (offset == null) {
            return null;
        }
        return this.translate(offset.getOffset());
    }

    @Override
    public Block translate(Vector3f offset) {
        return this.translate((int) offset.getX(), (int) offset.getY(), (int) offset.getZ());
    }

    @Override
    public Block translate(Vector3i offset) {
        return this.translate(offset.getX(), offset.getY(), offset.getZ());
    }

    @Override
    public Block translate(int dx, int dy, int dz) {
        return new FlowBlock(getWorld(), this.x + dx, this.y + dy, this.z + dz);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other != null && other instanceof Block) {
            Block b = (Block) other;
            return b.getWorld() == this.getWorld() && b.getX() == this.getX() && b.getY() == this.getY() && b.getZ() == this.getZ();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getWorld()).append(getX()).append(getY()).append(getZ()).toHashCode();
    }

    @Override
    public String toString() {
        return StringUtil.toNamedString(this, this.world.get(), this.x, this.y, this.z);
    }

    @Override
    public boolean setMaterial(BlockMaterial material, int data, Cause<?> cause) {
        FlowChunk chunk = this.getChunk();
        // TODO once stable, remove this
        if (!chunk.getWorld().getEngine().getPlatform().isServer()) {
            throw new UnsupportedOperationException("Temporary lockdown of setMaterial. Server only!");
        }
        return chunk.setBlockMaterial(x, y, z, material, (short) data, cause);
    }

    @Override
    public boolean setMaterial(BlockMaterial material, int data) {
        return setMaterial(material, data, null);
    }

    @Override
    public FlowBlock setData(BlockMaterial data) {
        return this.setData(data.getData());
    }

    @Override
    public FlowBlock setData(int data) {
        return setData(data, null);
    }

    @Override
    public FlowBlock setData(int data, Cause<?> cause) {
        this.getChunk().setBlockData(this.x, this.y, this.z, (short) data, cause);
        return this;
    }

    @Override
    public short getBlockData() {
        return this.getChunk().getBlockData(this.x, this.y, this.z);
    }

    @Override
    public short setDataBits(int bits) {
        return this.getChunk().setBlockDataBits(this.x, this.y, this.z, bits, null);
    }

    @Override
    public short setDataBits(int bits, boolean set) {
        return this.getChunk().setBlockDataBits(this.x, this.y, this.z, bits, set, null);
    }

    @Override
    public short clearDataBits(int bits) {
        return this.getChunk().clearBlockDataBits(this.x, this.y, this.z, bits, null);
    }

    @Override
    public int getDataField(int bits) {
        return this.getChunk().getBlockDataField(this.x, this.y, this.z, bits);
    }

    @Override
    public boolean isDataBitSet(int bits) {
        return this.getChunk().isBlockDataBitSet(this.x, this.y, this.z, bits);
    }

    @Override
    public int setDataField(int bits, int value) {
        return this.getChunk().setBlockDataField(this.x, this.y, this.z, bits, value, null);
    }

    @Override
    public int addDataField(int bits, int value) {
        return this.getChunk().addBlockDataField(this.x, this.y, this.z, bits, value, null);
    }

    @Override
    public FlowRegion getRegion() {
        return (FlowRegion) this.getChunk().getRegion();
    }

    @Override
    public BlockMaterial getMaterial() {
        return this.getChunk().getBlockMaterial(this.x, this.y, this.z);
    }

    @Override
    public boolean setMaterial(BlockMaterial material) {
        return this.setMaterial(material, material.getData());
    }

    @Override
    public boolean setMaterial(BlockMaterial material, Cause<?> cause) {
        return this.setMaterial(material, material.getData(), cause);
    }

    @Override
    public boolean isMaterial(Material... materials) {
        return getMaterial().isMaterial(materials);
    }

    @Override
    public <T extends Component> T add(Class<T> type) {
        return getChunk().getBlockComponentOwner(x, y, z, true).add(type);
    }

    @Override
    public <T extends Component> T detach(Class<? extends Component> type) {
        BlockComponentOwner owner = getChunk().getBlockComponentOwner(x, y, z, false);
        if (owner != null) {
            return owner.detach(type);
        }
        return null;
    }

    @Override
    public Collection<Component> values() {
        return getChunk().getBlockComponentOwner(x, y, z, true).values();
    }

    @Override
    public ManagedMap getData() {
        BlockComponentOwner owner = getChunk().getBlockComponentOwner(x, y, z, false);
        if (owner == null) {
            throw new IllegalStateException("The datatable is only available on blocks who have a BlockComponentOwner (blocks with components added)");
        }
        return owner.getData();
    }

    @Override
    public <T extends Component> T get(Class<T> type) {
        return getChunk().getBlockComponentOwner(x, y, z, true).get(type);
    }

    @Override
    public <T> T getType(Class<T> type) {
        return getChunk().getBlockComponentOwner(x, y, z, true).getType(type);
    }

    @Override
    public <T extends Component> T getExact(Class<T> type) {
        return getChunk().getBlockComponentOwner(x, y, z, true).getExact(type);
    }

    @Override
    public <T extends Component> Collection<T> getAll(Class<T> type) {
        return getChunk().getBlockComponentOwner(x, y, z, true).getAll(type);
    }

    @Override
    public <T> Collection<T> getAllOfType(Class<T> type) {
        return getChunk().getBlockComponentOwner(x, y, z, true).getAllOfType(type);
    }
}
