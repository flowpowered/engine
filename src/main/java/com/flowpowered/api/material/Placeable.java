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

import com.flowpowered.events.Cause;

import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.material.block.BlockFace;
import com.flowpowered.math.vector.Vector3f;

/**
 * An interface defining a {@link Material} that can be placed
 */
public interface Placeable {
    /**
     * Called when this block is about to be placed (before {@link #onPlacement(Block, short, BlockFace, boolean)}), checking if placement is allowed or not.
     *
     * @param block to place
     * @param data block data to use during placement
     * @param against face against the block is placed
     * @param isClickedBlock whether the block is to be placed at the clicked block
     * @param cause the cause of the placement
     * @return true if placement is allowed
     */
    public boolean canPlace(Block block, short data, BlockFace against, Vector3f clickedPos, boolean isClickedBlock, Cause<?> cause);

    /**
     * Called when this block is placed, handling the actual placement<br> This method should only change properties that rely on the face it is placed against, or in what way it is placed. All other
     * logic should be performed in onCreate.
     *
     * @param block to affect
     * @param data block data to use during placement
     * @param against face against the block is placed
     * @param clickedPos relative position the block was clicked to place this block
     * @param isClickedBlock whether the block is being placed at the clicked block
     * @param cause the cause of the placement
     */
    public void onPlacement(Block block, short data, BlockFace against, Vector3f clickedPos, boolean isClickedBlock, Cause<?> cause);
}
