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
package com.flowpowered.api.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;

/**
 * A world generator that generates using previously-specified layers of blocks
 */
public class LayeredWorldGenerator implements WorldGenerator {
    private List<Layer> layers = new ArrayList<>();
    private int minimum = Integer.MAX_VALUE;
    private int height = Integer.MIN_VALUE;
    private short floorid = 0, floordata = 0;

    @Override
    public void generate(CuboidBlockMaterialBuffer blockData, World world) {
        final int startY = blockData.getBase().getFloorY();
        final int endY = blockData.getTop().getFloorY();
        int y, height;
        for (Layer layer : this.layers) {
            if (layer.getTop() > startY && layer.getY() < endY) {
                y = Math.max(startY, layer.getY());
                height = Math.min(endY, layer.getTop()) - y;
                blockData.setHorizontalLayer(y, height, layer.getId(), layer.getData());
            }
        }
        // Floor layer
        if (startY < this.minimum) {
            height = Math.min(endY, this.minimum) - startY;
            blockData.setHorizontalLayer(startY, height, this.floorid, this.floordata);
        }
    }

    @Override
    public Populator[] getPopulators() {
        return new Populator[0];
    }

    @Override
    public String getName() {
        return "LayeredWorld";
    }

    /**
     * Gets the total height of all layers
     *
     * @return Layer height
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Gets an immutable list of layers specified for this layered World Generator
     *
     * @return List of layers
     */
    public List<Layer> getLayers() {
        return Collections.unmodifiableList(this.layers);
    }

    /**
     * Sets the floor layer material, the material for below the lowest layer<br> By default this layer is full of empty material (air)
     *
     * @param material of the layer
     */
    protected void setFloorLayer(BlockMaterial material) {
        this.setFloorLayer(material.getId(), material.getData());
    }

    /**
     * Sets the floor layer material, the material for below the lowest layer<br> By default this layer is full of empty material (air)
     *
     * @param id of the material of the layer
     * @param data of the layer
     */
    protected void setFloorLayer(short id, short data) {
        this.floorid = id;
        this.floordata = data;
    }

    /**
     * Stacks a new layer on top of a previous one<br> At least one layer added using addLayer should be defined before calling this method<br> Otherwise the y-coordinate of this layer will be incorrect
     *
     * @param height of the new layer
     * @param material of the layer
     */
    protected void stackLayer(int height, BlockMaterial material) {
        this.addLayer(this.height, height, material);
    }

    /**
     * Stacks a new layer on top of a previous one<br> At least one layer added using addLayer should be defined before calling this method<br> Otherwise the y-coordinate of this layer will be incorrect
     *
     * @param height of the new layer
     * @param id of the material of the layer
     * @param data of the layer
     */
    protected void stackLayer(int height, short id, short data) {
        this.addLayer(this.height, height, id, data);
    }

    /**
     * Adds a single layer
     *
     * @param y - coordinate of the start of the layer
     * @param height of the layer
     * @param material of the layer
     */
    protected void addLayer(int y, int height, BlockMaterial material) {
        this.addLayer(y, height, material.getId(), material.getData());
    }

    /**
     * Adds a single layer
     *
     * @param y - coordinate of the start of the layer
     * @param height of the layer
     * @param id of the material of the layer
     * @param data of the layer
     */
    protected void addLayer(int y, int height, short id, short data) {
        final Layer layer = new Layer(y, height, id, data);
        this.layers.add(layer);
        this.height = Math.max(this.height, layer.getTop());
        this.minimum = Math.min(this.minimum, layer.getY());
    }

    public static class Layer {
        private final short id, data;
        private final int y, height, topy;

        public Layer(int y, int height, short id, short data) {
            this.y = y;
            this.height = height;
            this.topy = y + height;
            this.id = id;
            this.data = data;
        }

        public int getY() {
            return y;
        }

        public int getHeight() {
            return height;
        }

        public int getTop() {
            return topy;
        }

        public short getId() {
            return id;
        }

        public short getData() {
            return data;
        }
    }
}
