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
package com.flowpowered.api.io.bytearrayarray;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class BAAOpenInProgress implements ByteArrayArray {
    private BAAOpenInProgress() {
    }

    private final static BAAOpenInProgress instance = new BAAOpenInProgress();

    public static BAAOpenInProgress getInstance() {
        return instance;
    }

    @Override
    public DataInputStream getInputStream(int i) throws IOException {
        return null;
    }

    @Override
    public DataOutputStream getOutputStream(int i) throws IOException {
        return null;
    }

    @Override
    public boolean attemptClose() throws IOException {
        return false;
    }

    @Override
    public boolean isTimedOut() {
        return false;
    }

    @Override
    public void closeIfTimedOut() throws IOException {
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean exists(int i) throws IOException {
        return false;
    }

    @Override
    public void delete(int i) throws IOException {
    }
}
