package org.spout.engine.geo;

import java.util.List;

import com.flowpowered.events.Cause;
import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.cuboid.Region;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFace;
import org.spout.api.scheduler.TaskManager;
import org.spout.api.util.cuboid.CuboidBlockMaterialBuffer;
import org.spout.math.vector.Vector3f;

public class SpoutRegion extends Region {

    public SpoutRegion(World world, float x, float y, float z) {
        super(world, x, y, z);
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unload(boolean save) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getAll() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Entity getEntity(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Player> getPlayers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TaskManager getTaskManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isLoaded() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getChunk(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getChunkFromBlock(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getChunkFromBlock(Vector3f position, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasChunk(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasChunkAtBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveChunk(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unloadChunk(int x, int y, int z, boolean save) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNumLoadedChunks() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean setBlockData(int x, int y, int z, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean addBlockData(int x, int y, int z, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean setBlockMaterial(int x, int y, int z, BlockMaterial material, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean compareAndSetData(int x, int y, int z, int expect, short data, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short setBlockDataBits(int x, int y, int z, int bits, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short setBlockDataBits(int x, int y, int z, int bits, boolean set, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short clearBlockDataBits(int x, int y, int z, int bits, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBlockDataField(int x, int y, int z, int bits) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isBlockDataBitSet(int x, int y, int z, int bits) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int setBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int addBlockDataField(int x, int y, int z, int bits, int value, Cause<?> source) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Block getBlock(float x, float y, float z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Block getBlock(Vector3f position) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean commitCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCuboid(int x, int y, int z, CuboidBlockMaterialBuffer buffer, Cause<?> cause) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(boolean backBuffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz, boolean backBuffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCuboid(int bx, int by, int bz, CuboidBlockMaterialBuffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCuboid(CuboidBlockMaterialBuffer buffer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BlockMaterial getBlockMaterial(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getBlockFullState(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public short getBlockData(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getLocalRegion(BlockFace face, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Region getLocalRegion(int dx, int dy, int dz, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(Chunk c, BlockFace face, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(Chunk c, int ox, int oy, int oz, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(int x, int y, int z, int ox, int oy, int oz, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Chunk getLocalChunk(int x, int y, int z, LoadOption loadopt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
