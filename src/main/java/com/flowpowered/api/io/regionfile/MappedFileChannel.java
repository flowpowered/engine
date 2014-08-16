package com.flowpowered.api.io.regionfile;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Util class used to keep track of memory mapping and paging of a FileChannel.
 */
public class MappedFileChannel {
    private final Path filePath;
    private final OpenOption[] options;
    private final ArrayList<MappedByteBuffer> pages = new ArrayList<>();
    private final int PAGE_SHIFT;
    private final int PAGE_SIZE;
    private final long PAGE_MASK;
    private FileChannel channel;

    public MappedFileChannel(Path filePath, OpenOption... options) throws IOException {
        this(filePath, 17, options);
    }

    public MappedFileChannel(Path filePath, int pageShift, OpenOption... options) throws IOException {
        this.channel = FileChannel.open(filePath, options);
        this.PAGE_SHIFT = pageShift;
        PAGE_SIZE = (1 << PAGE_SHIFT);
        PAGE_MASK = PAGE_SIZE - 1;
        this.filePath = filePath;
        this.options = options;
    }

    public long length() throws IOException {
        return channel.size();
    }

    public void close() throws IOException {
        for (MappedByteBuffer m : pages) {
            if (m != null) {
                m.force();
            }
        }
        channel.close();
    }

    byte[] intArray = new byte[4];

    public void writeInt(int i) throws IOException {
        intArray[0] = (byte) (i >> 24);
        intArray[1] = (byte) (i >> 16);
        intArray[2] = (byte) (i >> 8);
        intArray[3] = (byte) (i);
        write(intArray, 0, 4);
    }

    public int readInt() throws IOException {
        readFully(intArray);
        int i = 0;
        i |= (intArray[0] & 0xFF) << 24;
        i |= (intArray[1] & 0xFF) << 16;
        i |= (intArray[2] & 0xFF) << 8;
        i |= (intArray[3] & 0xFF);
        return i;
    }

    private MappedByteBuffer getPage(int pageIndex) throws IOException {
        while (pageIndex >= pages.size()) {
            pages.add(null);
        }
        MappedByteBuffer page = pages.get(pageIndex);
        if (page == null) {
            long pagePosition = pageIndex << PAGE_SHIFT;
            boolean interrupted = false;
            boolean success = false;
            try {
                while (!success) {
                    try {
                        interrupted |= Thread.interrupted();
                        page = channel.map(FileChannel.MapMode.READ_WRITE, pagePosition, PAGE_SIZE);
                        success = true;
                    } catch (ClosedByInterruptException e) {
                        channel = FileChannel.open(filePath, options);
                    } catch (IOException e) {
                        throw new IOException("Unable to refresh RandomAccessFile after interrupt, " + filePath, e);
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            pages.set(pageIndex, page);
        }
        return page;
    }

    public void seek(long pos) throws IOException {
        channel.position(pos);
    }

    public void readFully(byte[] b) throws IOException {
        long pos = channel.position();
        int pageIndex = (int) (pos >> PAGE_SHIFT);
        int offset = (int) (pos & PAGE_MASK);
        int endPageOne = Math.min(b.length + offset, PAGE_SIZE);

        MappedByteBuffer page = getPage(pageIndex);

        int j = 0;

        int length = endPageOne - offset;

        page.position(offset);

        page.get(b, j, length);
        j += length;

        while (b.length > j) {
            pageIndex++;
            page = getPage(pageIndex);
            page.position(0);
            if (b.length - j > PAGE_SIZE) {
                length = PAGE_SIZE;
            } else {
                length = b.length - j;
            }

            page.get(b, j, length);
            j += length;
        }

        pos += b.length;
        channel.position(pos);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        long pos = channel.position();
        int pageIndex = (int) (pos >> PAGE_SHIFT);
        int offset = (int) (pos & PAGE_MASK);
        int endPageOne = Math.min(len + offset, PAGE_SIZE);

        MappedByteBuffer page = getPage(pageIndex);

        int j = 0;

        int length = endPageOne - offset;

        page.position(offset);

        page.put(b, off + j, length);
        j += length;

        while (len > j) {
            pageIndex++;
            page = getPage(pageIndex);
            page.position(0);
            if (len - j > PAGE_SIZE) {
                length = PAGE_SIZE;
            } else {
                length = len - j;
            }
            page.put(b, off + j, length);
            j += length;
        }

        pos += len;
        channel.position(pos);
    }
}
