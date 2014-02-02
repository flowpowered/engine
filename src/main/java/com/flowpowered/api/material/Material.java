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
package com.flowpowered.api.material;

/**
 * Defines the characteristics of Blocks or Items.
 */

import com.flowpowered.commons.LogicUtil;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.math.GenericMath;

public abstract class Material extends MaterialRegistry {
    private final short id;
    private final short data;
    private final String name;
    private final boolean isSubMaterial;
    private final Material parent;
    private final Material root;
    private String displayName;
    private int maxStackSize = 64;
    private short maxData = Short.MAX_VALUE;
    private final AtomicReference<Material[]> subMaterials;
    private Material[] submaterialsContiguous = null;
    private volatile boolean submaterialsDirty = true;
    private final short dataMask;

    /**
     * Creates a material with a dataMask, name
     */
    public Material(short dataMask, String name) {
        this.isSubMaterial = false;
        this.displayName = name;
        this.name = getClass().getCanonicalName() + "_" + name.replace(' ', '_');
        this.parent = this;
        this.data = 0;
        this.id = (short) MaterialRegistry.register(this);
        this.subMaterials = MaterialRegistry.getSubMaterialReference(this.id);
        this.dataMask = dataMask;
        this.root = this;
    }

    /**
     * Creates and registers a material
     *
     * @param name of the material
     */
    public Material(String name) {
        this((short) 0, name);
    } 

    /**
     * Creates and registers a sub material
     *
     * @param name of the material
     * @param parent material
     */
    public Material(String name, int data, Material parent) {
        this(name, data, parent, null);
    }

    /**
     * Creates and registers a sub material
     *
     * @param name of the material
     * @param parent material
     */
    public Material(String name, int data, Material parent, String model) {
        this.isSubMaterial = true;
        this.displayName = name;
        this.name = name.replace(' ', '_');
        this.parent = parent;
        this.data = (short) data;
        this.id = (short) MaterialRegistry.register(this);
        this.subMaterials = MaterialRegistry.getSubMaterialReference(this.id);
        this.dataMask = parent.getDataMask();
        this.root = parent.getRoot();
    }

    /**
     * Creates a material with a reserved id
     *
     * @param name of the material
     * @param id to reserve
     */
    protected Material(String name, short id) {
        this.isSubMaterial = false;
        this.displayName = name;
        this.name = name.replace(' ', '_');
        this.parent = this;
        this.data = 0;
        this.id = (short) MaterialRegistry.register(this, id);
        this.subMaterials = MaterialRegistry.getSubMaterialReference(this.id);
        this.dataMask = 0;
        this.root = this;
    }

    public final short getId() {
        return this.id;
    }

    /**
     * Gets the data value associated with this material. if this material does not have or is not a sub material, then (getData() & getDataMask()) is equal to zero.
     *
     * @return data value
     */
    public final short getData() {
        return this.data;
    }

    /**
     * Gets the data mask for this material, and sub-materials.  When determining sub-material, this mask is applied to the data before the comparison is performed.
     *
     * @return data mask
     */
    public final short getDataMask() {
        return this.dataMask;
    }

    /**
     * Checks if this material is a sub material or not
     *
     * @return true if it is a sub material
     */
    public final boolean isSubMaterial() {
        return isSubMaterial;
    }

    /**
     * Checks if this material has other materials mapped by data
     *
     * @return true if this material has sub materials
     */
    public final boolean hasSubMaterials() {
        return this.subMaterials.get().length > 1;
    }

    /**
     * Gets all sub materials of this material
     *
     * @return an array of sub materials
     */
    public final Material[] getSubMaterials() {
        if (submaterialsDirty) {
            int materialCount = 0;
            Material[] sm = subMaterials.get();
            for (int i = 0; i < sm.length; i++) {
                if (sm[i] != null) {
                    materialCount++;
                }
            }
            Material[] newSubmaterials = new Material[materialCount];
            materialCount = 0;
            for (int i = 0; i < sm.length; i++) {
                if (sm[i] != null) {
                    newSubmaterials[materialCount++] = sm[i];
                }
            }
            this.submaterialsContiguous = newSubmaterials;
            submaterialsDirty = false;
        }
        Material[] sm = submaterialsContiguous;
        return Arrays.copyOf(sm, sm.length);
    }

    /**
     * Recursively gets the sub material mapped to the data value specified
     *
     * @param data to search for
     * @return the sub material, or this material if not found
     */
    public Material getSubMaterial(short data) {
        short maskedData = (short) (data & dataMask);
        return subMaterials.get()[maskedData];
    }

    /**
     * Registers the sub material for this material
     *
     * @param material to register
     */
    public final void registerSubMaterial(Material material) {
        submaterialsDirty = true;
        try {
            int data = material.data & 0xFFFF;
            if ((data & dataMask) != data) {
                throw new IllegalArgumentException("Sub material of: " + material.getId() + " with data value: " + data + " is outside data mask: " + Integer.toHexString(dataMask));
            }
            if (material.isSubMaterial) {
                if (material.getParentMaterial() == this) {
                    boolean success = false;
                    while (!success) {
                        Material[] sm = subMaterials.get();
                        if (data >= sm.length) {
                            int newSize = GenericMath.roundUpPow2(data + (data >> 1) + 1);
                            Material[] newSubmaterials = new Material[newSize];
                            System.arraycopy(sm, 0, newSubmaterials, 0, sm.length);
                            success = subMaterials.compareAndSet(sm, newSubmaterials);
                        } else {
                            success = true;
                        }
                    }
                    Material[] sm = subMaterials.get();
                    if (sm[data] == null) {
                        sm[data] = material;
                    } else {
                        throw new IllegalArgumentException("Two sub material registered for the same data value");
                    }
                } else {
                    throw new IllegalArgumentException("Sub Material is registered to a material different than the parent!");
                }
            } else {
                throw new IllegalArgumentException("Material is not a valid sub material!");
            }
        } finally {
            submaterialsDirty = true;
        }
    }

    /**
     * Gets the parent of this sub material
     *
     * @return the material of the parent
     */
    public Material getParentMaterial() {
        return this.parent;
    }

    /**
     * Gets the root parent of this sub material
     *
     * @return the material root
     */
    public Material getRoot() {
        return this.root;
    }

    /**
     * Gets the name of this material
     *
     * @return the name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Gets the display name of this material
     *
     * @return the display name
     */
    public final String getDisplayName() {
        return this.displayName;
    }

    /**
     * Sets the display name of this material
     *
     * @param name the new display name
     */
    public final void setDisplayName(String name) {
        this.displayName = name;
    }

    /**
     * Gets the maximum size of a stack of this material
     *
     * @return the current max size
     */
    public final int getMaxStackSize() {
        return this.maxStackSize;
    }

    /**
     * Sets the maximum size of a stack of this material
     *
     * @param newValue the new maximum stack size
     */
    public final void setMaxStackSize(int newValue) {
        this.maxStackSize = newValue;
    }

    /**
     * Gets the maximum data a stack of this material can have
     */
    public final short getMaxData() {
        return this.maxData;
    }

    /**
     * Sets the maximum of the data value this material can have
     *
     * @param newValue the new maximum data
     */
    public final void setMaxData(short newValue) {
        this.maxData = newValue;
    }

    public boolean isMaterial(Material... materials) {
        if (LogicUtil.equalsAny(this, materials)) {
            return true;
        }
        if (this.getRoot() != this && LogicUtil.equalsAny(this.getRoot(), materials)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Material) {
            return other == this;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Material {" + getName() + "}";
    }

    /**
     * Indicates that the dataMask covers the least significant bits.<br> <br> This method is used when verifying that the dataMask is set correctly
     */
    public boolean hasLSBDataMask() {
        return true;
    }
}
