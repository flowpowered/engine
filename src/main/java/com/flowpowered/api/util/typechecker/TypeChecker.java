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
package com.flowpowered.api.util.typechecker;

import org.spout.nbt.Tag;

public class TypeChecker<T> extends com.flowpowered.commons.typechecker.TypeChecker<T> {

    public TypeChecker(Class<T> clazz) {
        super(clazz);
    }

    /**
     * Checks and casts an object contained in a tag to the specified type.
     *
     * @param tag The Tag containing the object to be checked
     *
     * @return The object contained in the tag, cast to the specified class
     *
     * @throws ClassCastException if casting fails
     */
    public final T checkTag(Tag<?> tag) {
        return check(tag.getValue());
    }

    /**
     * Checks and casts an object to the specified type. If casting fails, a default value is returned.
     *
     * @param tag          The Tag containing the object to be checked
     * @param defaultValue The default value to be returned if casting fails
     *
     * @return The object contained in the tag, cast to the specified class, or the default value, if casting fails
     */
    public final T checkTag(Tag<?> tag, T defaultValue) {
        if (tag == null) {
            return defaultValue;
        }

        try {
            return check(tag.getValue());
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}
