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
package com.flowpowered.api.geo.cuboid;

public enum UpdateOption {
	/**
	 * Updates the block itself
	 */
	SELF(true, false),
	/**
	 * Updates the blocks surrounding this block
	 */
	AROUND(false, true),
	/**
	 * Updates the block and the blocks surrounding the blocks
	 */
	SELF_AROUND(true, true);

	private final boolean self;
	private final boolean around;

	private UpdateOption(boolean self, boolean around) {
		this.self = self;
		this.around = around;
	}

	/**
	 * Test if chunk/region should be loaded if not currently loaded
	 *
	 * @return true if yes, false if no
	 */
	public final boolean updateSelf() {
		return self;
	}

	/**
	 * Test if chunk/region should be generated if it does not exist
	 *
	 * @return true if yes, false if no
	 */
	public final boolean updateAround() {
		return around;
	}

}
