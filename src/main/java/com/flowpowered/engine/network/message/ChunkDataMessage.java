package com.flowpowered.engine.network.message;

import com.flowpowered.networking.Message;

public class ChunkDataMessage implements Message {
	private final boolean unload;
	// Block x, y, z
	private final int x, y, z;
	private final int[] blocks;

	public ChunkDataMessage(int x, int y, int z) {
		this.unload = true;
		this.x = x;
		this.y = y;
		this.z = z;
		this.blocks = null;
	}

	public ChunkDataMessage(int x, int y, int z, int[] blocks) {
		this.unload = false;
		this.x = x;
		this.y = y;
		this.z = z;
		this.blocks = blocks;
	}

	public boolean isUnload() {
		return unload;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public int[] getBlocks() {
		return blocks;
	}
}
