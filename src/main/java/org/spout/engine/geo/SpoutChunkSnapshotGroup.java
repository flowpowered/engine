package org.spout.engine.geo;

import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.ChunkSnapshot;
import org.spout.api.geo.cuboid.ChunkSnapshotGroup;
import org.spout.api.material.BlockMaterial;

public class SpoutChunkSnapshotGroup implements ChunkSnapshotGroup {
	private final int cx, cy, cz;
	private final ChunkSnapshot[][][] chunks;

    public SpoutChunkSnapshotGroup(int cx, int cy, int cz, ChunkSnapshot[][][] chunks) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.chunks = chunks;
    }

    @Override
    public int getX() {
        return cx;
    }

    @Override
    public int getY() {
        return cy;
    }

    @Override
    public int getZ() {
        return cz;
    }

    @Override
    public boolean isUnload() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChunkSnapshot getCenter() {
        return chunks[1][1][1];
    }

    @Override
    public void cleanUp() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	/**
	 * Gets the chunk at world chunk coordinates<br> Note: Coordinates must be within this model, or index out of bounds will be thrown.
	 *
	 * @param worldX coordinate of the chunk
	 * @param worldY coordinate of the chunk
	 * @param worldZ coordinate of the chunk
	 * @return The chunk, or null if not available
	 */
	@Override
	public ChunkSnapshot getChunk(int worldX, int worldY, int worldZ) {
		return chunks[worldX - this.cx + 1][worldY - this.cy + 1][worldZ - this.cz + 1];
	}

    @Override
    public ChunkSnapshot getChunkFromBlock(int bx, int by, int bz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BlockMaterial getBlock(int localX, int localY, int localZ) {
        ChunkSnapshot chunk = getChunk(localX >> Chunk.BLOCKS.BITS, localY >> Chunk.BLOCKS.BITS, localZ >> Chunk.BLOCKS.BITS);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockMaterial(localX, localY, localZ);
    }
}
