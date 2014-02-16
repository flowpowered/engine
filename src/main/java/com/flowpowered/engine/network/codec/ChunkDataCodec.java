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
package com.flowpowered.engine.network.codec;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.flowpowered.api.geo.cuboid.Chunk;
import com.flowpowered.engine.network.message.ChunkDataMessage;
import com.flowpowered.networking.Codec;

import io.netty.buffer.ByteBuf;

public class ChunkDataCodec implements Codec<ChunkDataMessage> {
	private static final byte ISUNLOAD = 0b1;
	private static final int INTIAL_DATA_SIZE = Chunk.BLOCKS.VOLUME * 2 + Chunk.BLOCKS.VOLUME * 2; // Block Ids, Block Data

	@Override
	public ByteBuf encode(ByteBuf buffer, ChunkDataMessage message) throws IOException {
		if (message.isUnload()) {
			buffer.writeByte(ISUNLOAD); // we're unloading
			buffer.writeInt(message.getX());
			buffer.writeInt(message.getY());
			buffer.writeInt(message.getZ());
		} else {
			int dataSize = INTIAL_DATA_SIZE;

			byte[] uncompressedData = new byte[dataSize];
			byte[] compressedData = new byte[dataSize];

			int index = 0;
			for (int i : message.getBlocks()) {
				uncompressedData[index++] = (byte) i;
				uncompressedData[index++] = (byte) (i >> 8);
				uncompressedData[index++] = (byte) (i >> 16);
				uncompressedData[index++] = (byte) (i >> 24);
			}

			Deflater deflater = new Deflater();
			deflater.setInput(uncompressedData);
			deflater.finish();
			int compressedSize = deflater.deflate(compressedData);
			deflater.end();

			if (compressedSize == 0) {
				throw new IOException("Not all data compressed!");
			}

			buffer.writeByte(0); // Not unload
			buffer.writeInt(message.getX());
			buffer.writeInt(message.getY());
			buffer.writeInt(message.getZ());
			buffer.writeInt(compressedSize);
			buffer.writeBytes(compressedData, 0, compressedSize);
		}
		return buffer;
	}

	@Override
	public ChunkDataMessage decode(ByteBuf buffer) throws IOException {
		final byte info = buffer.readByte();
		final boolean unload = (info & ISUNLOAD) == ISUNLOAD;
		final int x = buffer.readInt();
		final int y = buffer.readInt();
		final int z = buffer.readInt();
		if (unload) {
			return new ChunkDataMessage(x, y, z);
		} else {
			int uncompressedSize = INTIAL_DATA_SIZE;
			final byte[] uncompressedData = new byte[uncompressedSize];
			final byte[] compressedData = new byte[buffer.readInt()];
			buffer.readBytes(compressedData);
			Inflater inflater = new Inflater();
			inflater.setInput(compressedData);
			try {
				inflater.inflate(uncompressedData);
			} catch (DataFormatException e) {
				throw new IOException("Error while reading chunk (" + x + "," + y + "," + z + ")!", e);
			}
			inflater.end();

			final int[] blocks = new int[Chunk.BLOCKS.VOLUME];

			int index = 0;
			for (int i = 0; i < blocks.length; ++i) {
				blocks[i] = uncompressedData[index++] | (uncompressedData[index++] << 8) | (uncompressedData[index++] << 16) | (uncompressedData[index++] << 24);
			}

			if (index != uncompressedData.length) {
				throw new IllegalStateException("Incorrect parse size - actual:" + index + " expected: " + uncompressedData.length);
			}

			return new ChunkDataMessage(x, y, z, blocks);
		}
	}
}
