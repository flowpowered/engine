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
package com.flowpowered.api.geo;

public class LoadOption {
    /**
     * Do not load or generate chunk/region if not currently loaded
     */
    public static final LoadOption NO_LOAD = new LoadOption(false, false, true);
    /**
     * Load chunk/region if not currently loaded, but do not generate it if it does not yet exist
     */
    public static final LoadOption LOAD_ONLY = new LoadOption(true, false, true);
    /**
     * Load chunk/region if not currently loaded, and generate it if it does not yet exist
     */
    public static final LoadOption LOAD_GEN = new LoadOption(true, true, true);
    /**
     * Don't load the chunk if it has already been generated, only generate if it does not yet exist
     */
    public static final LoadOption GEN_ONLY = new LoadOption(false, true, true);
    /**
     * Load chunk/region if not currently loaded, and generate it if it does not yet exist. Do not wait for generation to finish.
     */
    public static final LoadOption LOAD_GEN_NOWAIT = new LoadOption(true, true, false);

    private final boolean load;
    private final boolean generate;
    private final boolean wait;

    public LoadOption(boolean load, boolean generate, boolean wait) {
        this.load = load;
        this.generate = generate;
        this.wait = wait;
    }

    /**
     * Test if chunk/region should be loaded if not currently loaded
     *
     * @return true if yes, false if no
     */
    public final boolean loadIfNeeded() {
        return load;
    }

    /**
     * Test if chunk/region should be generated if it does not exist
     *
     * @return true if yes, false if no
     */
    public final boolean generateIfNeeded() {
        return generate;
    }

    public boolean isWait() {
        return wait;
    }

}
