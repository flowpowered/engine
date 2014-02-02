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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.io.regionfile.SimpleRegionFile;

public class BAAWrapper {
    private final static ByteArrayArray openInProgress = BAAOpenInProgress.getInstance();
    private AtomicReference<ByteArrayArray> baaRef = new AtomicReference<>(null);
    private final File file;
    private final int segmentSize;
    private final int entries;
    private final int timeout;

    public BAAWrapper(File file, int segmentSize, int entries, int timeout) {
        this.file = file;
        this.segmentSize = segmentSize;
        this.entries = entries;
        this.timeout = timeout;
    }

    /**
     * This method should be called periodically in order to see if the ByteArrayArray has timed out.  It always returns immediately.<br> <br> It will only close the array if no block OutputStreams are
     * open and the last access occurred more than the timeout previously
     */
    public void timeoutCheck() {
        ByteArrayArray baa = baaRef.get();
        if (baa != null) {
            try {
                baa.closeIfTimedOut();
            } catch (IOException ioe) {
            }
            if (baa.isClosed()) {
                baaRef.compareAndSet(baa, null);
            }
        }
    }

    /**
     * This method should be called periodically in order to see if the ByteArrayArray has timed out.  It always returns immediately.<br> <br> It will only close the array if no block OutputStreams are
     * open and the last access occurred more than the timeout previously
     *
     * @return true if the file is closed, or not opened
     */
    public boolean attemptClose() {
        ByteArrayArray baa = baaRef.get();
        if (baa != null) {
            try {
                boolean success = baa.attemptClose();
                if (baa.isClosed()) {
                    baaRef.compareAndSet(baa, null);
                }
                return success;
            } catch (IOException ioe) {
            }
        }
        return true;
    }

    /**
     * Checks if the InputStream exists at the given index.
     *
     * @param i the block index
     * @return true if the input stream exists
     */
    public boolean inputStreamExists(int i) {
        while (true) {
            ByteArrayArray baa = getByteArrayArray();
            if (baa == null) {
                return false;
            }
            if (baa == openInProgress) {
                continue;
            }
            try {
                return baa.exists(i);
            } catch (BAAClosedException e) {
                continue;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * Checks if the InputStream exists at the given index.
     *
     * @param i the block index
     * @return true if the delete was successful
     */
    public boolean delete(int i) {
        while (true) {
            ByteArrayArray baa = getByteArrayArray();
            if (baa == null) {
                return false;
            }
            if (baa == openInProgress) {
                continue;
            }
            try {
                baa.delete(i);
                return true;
            } catch (BAAClosedException e) {
                continue;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * Gets the DataOutputStream corresponding to a given block.<br> <br> WARNING: This block will be locked until the stream is closed
     *
     * @param i the block index
     * @return the DataOutputStream
     */
    public OutputStream getBlockOutputStream(int i) {
        while (true) {
            ByteArrayArray baa = getByteArrayArray();
            if (baa == null) {
                return null;
            }
            OutputStream out;
            try {
                out = baa.getOutputStream(i);
            } catch (BAAClosedException e) {
                continue;
            } catch (IOException e) {
                return null;
            }
            return out;
        }
    }

    /**
     * Gets the DataInputStream corresponding to a given Chunk.<br> <br> The stream is based on a snapshot of the array.
     *
     * @param i the block index
     * @return the DataInputStream
     */
    public InputStream getBlockInputStream(int i) {
        while (true) {
            ByteArrayArray baa = getByteArrayArray();
            if (baa == null) {
                return null;
            }
            InputStream in;
            try {
                in = baa.getInputStream(i);
            } catch (BAAClosedException e) {
                continue;
            } catch (IOException e) {
                return null;
            }
            return in;
        }
    }

    /**
     * Gets the filename of the file handled by this wrapper
     *
     * @return the filename
     */
    public String getFilename() {
        return file.getPath();
    }

    private ByteArrayArray getByteArrayArray() {
        int count = 0;
        while (true) {
            ByteArrayArray baa = baaRef.get();

            if (baa != null) {
                // If the baa exists and isn't closed return it
                if (!baa.isClosed()) {
                    return baa;
                }
                baaRef.compareAndSet(baa, null);
                continue;
            }

            if (baaRef.compareAndSet(null, openInProgress)) {
                // Successfully claimed the right to open a new file
                // Attempt to open the file.  If an IOException is throw return null
                baa = null; // not needed - already null
                try {
                    try {
                        baa = new SimpleRegionFile(file, segmentSize, entries, timeout);
                    } catch (IOException e) {
                        e.printStackTrace();
                        baa = null; // not needed - already null. The assignment above comes after the potential IOException. 
                    }

                    return baa;
                } finally {
                    if (!baaRef.compareAndSet(openInProgress, baa)) {
                        throw new IllegalStateException("chunkStore variable changed outside locking scheme");
                    }
                }
            }

            // Some other thread is trying to open the file
            // Spinning lock, then yield and then sleep
            count++;
            if (count > 10) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            } else if (count > 0) {
                Thread.yield();
            }
        }
    }
}
