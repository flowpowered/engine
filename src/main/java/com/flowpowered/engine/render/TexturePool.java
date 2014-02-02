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
package com.flowpowered.engine.render;

import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.InternalFormat;

/**
 *
 */
public class TexturePool {
    public Texture request(int width, int height, InternalFormat format) {
        return null;
    }

    /**
     * Checks if a candidate format can be used instead of the desired format. -1 is returned when that's not possible. Else, 0 is returned for a perfect match. The larger the value, the less optimal
     * the match.
     *
     * @param desired The desired format
     * @param candidate The candidate format
     * @return -1 if the candidate format doesn't match at all, 0 if the match is perfect or larger as the matches get less optimal
     */
    private static float checkMatch(InternalFormat desired, InternalFormat candidate) {
        if (candidate.getComponentCount() < desired.getComponentCount()
                || desired.hasRed() && !candidate.hasRed()
                || desired.hasGreen() && !candidate.hasGreen()
                || desired.hasBlue() && !candidate.hasBlue()
                || desired.hasAlpha() && !candidate.hasAlpha()
                || desired.hasDepth() && !candidate.hasDepth()
                || desired.isFloatBased() && !candidate.isFloatBased()) {
            return -1;
        }
        float match = 0;
        if (candidate.hasRed() && !desired.hasRed()) {
            match++;
        }
        if (candidate.hasGreen() && !desired.hasGreen()) {
            match++;
        }
        if (candidate.hasBlue() && !desired.hasBlue()) {
            match++;
        }
        if (candidate.hasAlpha() && !desired.hasAlpha()) {
            match++;
        }
        if (candidate.hasDepth() && !desired.hasDepth()) {
            match++;
        }
        if (candidate.isFloatBased() && !desired.isFloatBased()) {
            match++;
        }
        final float byteRatio = candidate.getBytesPerComponent() / (float) desired.getBytesPerComponent();
        if (byteRatio < 1) {
            return -1;
        } else {
            return match + byteRatio - 1;
        }
    }
}