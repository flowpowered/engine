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
package com.flowpowered.engine.geo.chunk;

import java.util.List;
import java.util.Set;

import gnu.trove.map.hash.TShortObjectHashMap;

import com.flowpowered.commons.datatable.ManagedHashMap;
import com.flowpowered.commons.hashing.NibbleQuadHashed;
import com.flowpowered.commons.store.block.AtomicBlockStore;
import com.flowpowered.events.Cause;

import com.flowpowered.api.component.BlockComponentOwner;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.BlockContainer;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.engine.geo.FlowBlock;
import com.flowpowered.engine.geo.region.FlowRegion;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3f;

public class FlowChunk extends Chunk {

    private final FlowRegion region;
    /**
     * Data map and Datatable associated with it
     */
    protected final ManagedHashMap dataMap;
    /**
     * Not thread safe, synchronize on access
     */
    private final TShortObjectHashMap<BlockComponentOwner> blockComponents = new TShortObjectHashMap<>();
    private final int generationIndex;
    /**
     * Storage for block ids, data and auxiliary data. For blocks with data = 0 and auxiliary data = null, the block is stored as a short.
     */
    protected final AtomicBlockStore blockStore;

    public FlowChunk(FlowRegion region, World world, int x, int y, int z, int generationIndex, AtomicBlockStore blockStore) {
        super(world, x << BLOCKS.BITS, y << BLOCKS.BITS, z << BLOCKS.BITS);
        this.region = region;
        this.dataMap = new ManagedHashMap();
        this.generationIndex = generationIndex;
        this.blockStore = blockStore;
    }

    @Override
    public void unload(boolean save) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void fillBlockContainer(BlockContainer container) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean refreshObserver(Entity player) {
        return true;
    }

    @Override
    public boolean removeObserver(Entity player) {
        return true;
    }

    @Override
    public FlowRegion getRegion() {
        return region;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean populate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean populate(boolean force) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void populate(boolean sync, boolean observe) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void populate(boolean sync, boolean observe, boolean priority) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isPopulated() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getEntities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getLiveEntities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNumObservers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<? extends Player> getObservingPlayers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<? extends Entity> getObservers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getGenerationIndex() {
        return generationIndex;
    }

    @Override
    public boolean setBlockData(int x, int y, int z, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean setBlockMaterial(int x, int y, int z, BlockMaterial material, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean compareAndSetData(int x, int y, int z, int expect, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short setBlockDataBits(int x, int y, int z, int bits, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short setBlockDataBits(int x, int y, int z, int bits, boolean set, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short clearBlockDataBits(int x, int y, int z, int bits, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBlockDataField(int x, int y, int z, int bits) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isBlockDataBitSet(int x, int y, int z, int bits) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int setBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int addBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FlowBlock getBlock(float x, float y, float z) {
        return new FlowBlock((FlowWorld) getWorld(), GenericMath.floor(x), GenericMath.floor(y), GenericMath.floor(z));
    }

    @Override
    public FlowBlock getBlock(Vector3f position) {
        return this.getBlock(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean commitCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCuboid(int x, int y, int z, CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(boolean backBuffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz, boolean backBuffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCuboid(int bx, int by, int bz, CuboidBlockMaterialBuffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCuboid(CuboidBlockMaterialBuffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BlockMaterial getBlockMaterial(int x, int y, int z) {
        int state = blockStore.getFullData(x & BLOCKS.MASK, y & BLOCKS.MASK, z & BLOCKS.MASK);
        return BlockMaterial.get(state);
    }

    @Override
    public int getBlockFullState(int x, int y, int z) {
        return blockStore.getFullData(x & BLOCKS.MASK, y & BLOCKS.MASK, z & BLOCKS.MASK);
    }

    @Override
    public short getBlockData(int x, int y, int z) {
        return blockStore.getData(x & BLOCKS.MASK, y & BLOCKS.MASK, z & BLOCKS.MASK);
    }

    public AtomicBlockStore getBlockStore() {
        return blockStore;
    }

    public BlockComponentOwner getBlockComponentOwner(int x, int y, int z, boolean create) {
        synchronized (blockComponents) {
            short packed = NibbleQuadHashed.key(x, y, z, 0);
            BlockComponentOwner value = blockComponents.get(packed);
            if (value == null && create) {
                value = new BlockComponentOwner(dataMap, NibbleQuadHashed.key1(packed), NibbleQuadHashed.key2(packed), NibbleQuadHashed.key3(packed), getWorld());
                blockComponents.put(packed, value);
            }
            return value;
        }
    }
}
