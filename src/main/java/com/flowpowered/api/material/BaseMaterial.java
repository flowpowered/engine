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

public abstract class BaseMaterial {

	private final int id;
	private final String name;
	private String displayName;
	private int maxStackSize = 64;
	private MATERIAL_STATE materialState = MATERIAL_STATE.UNDEFINED;

	public static enum MATERIAL_STATE {
		SOLID, LIQUID, GAS, UNDEFINED
	}

	/**
	 * Creates a material with a name
	 */
	public BaseMaterial(String name) {
		this.displayName = name;
		this.name = getClass().getCanonicalName() + "_" + name.replace(' ', '_');
		this.id = (short) MaterialRegistry.register(this);
	}

	/**
	 * Creates a material with a reserved id
	 *
	 * @param name of the material
	 * @param id   to reserve
	 */
	protected BaseMaterial(String name, int id) {
		this.displayName = name;
		this.name = name.replace(' ', '_');
		this.id = MaterialRegistry.register(this, id);
	}

	public final int getId() {
		return this.id;
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

	public boolean isMaterial(BaseMaterial... baseMaterials) {
		return LogicUtil.equalsAny(this, baseMaterials);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BaseMaterial && other == this;
	}

	@Override
	public String toString() {
		return "BaseMaterial {" + getName() + "}";
	}

	/**
	 * Indicates that the dataMask covers the least significant bits.<br> <br> This method is used when verifying that the dataMask is set correctly
	 */
	public boolean hasLSBDataMask() {
		return true;
	}

	/**
	 * Get the state of the Material
	 *
	 * @return
	 */
	public MATERIAL_STATE getMaterialState() {
		return materialState;
	}

	/**
	 * Set the state of the Material
	 *
	 * @param materialState to set
	 */
	public void setMaterialState(final MATERIAL_STATE materialState) {
		this.materialState = materialState;
	}
}
