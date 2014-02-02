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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Classes which implement this interface provide thread safe persistent storage for an array of byte arrays.<br> <br> Each entry of the array is referred to as a block.  Each block is a byte
 * array.<br> <br> The number of blocks in the array is determined at creation.
 */
public interface ByteArrayArray {
    /**
     * Gets a DataInputStream for reading a block.<br> <br> This method creates a snapshot of the block.
     *
     * @param i the index of the block
     * @return a DataInputStream for the block
     * @throws IOException on error
     */
    public InputStream getInputStream(int i) throws IOException;

    /**
     * Gets a DataOutputStream for writing to a block.<br> <br> WARNING:  This locks the block until the output stream is closed.<br>
     *
     * @param i the block index
     * @return a DataOutputStream for the block
     */
    public OutputStream getOutputStream(int i) throws IOException;

    /**
     * Attempts to close the map.  This method will only succeed if no block DataOutputStreams are active.
     *
     * @return true on success
     */
    public boolean attemptClose() throws IOException;

    /**
     * Checks if the access timeout has expired
     *
     * @return true on timeout
     */
    public boolean isTimedOut();

    /**
     * Attempts to close map if the file has timed out.<br> <br> This will fail if there are any open DataOutputStreams
     */
    public void closeIfTimedOut() throws IOException;

    /**
     * Gets if the map is closed
     *
     * @return true if the file is closed
     */
    public boolean isClosed();

    /**
     * Checks if any data exists at the block index.
     *
     * @param i the block index
     * @return true if it exists
     */
    boolean exists(int i) throws IOException;

    /**
     * Deletes the data at the block index.
     *
     * @param i the block index
     */
    void delete(int i) throws IOException;
}
