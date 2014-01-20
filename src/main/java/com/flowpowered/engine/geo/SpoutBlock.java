/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package com.flowpowered.engine.geo;

import com.flowpowered.engine.geo.chunk.SpoutChunk;
import com.flowpowered.engine.geo.world.SpoutWorld;
import com.flowpowered.engine.geo.region.SpoutRegion;
import java.lang.ref.WeakReference;
import java.util.Collection;

import com.flowpowered.commons.StringUtil;
import com.flowpowered.commons.datatable.ManagedMap;
import com.flowpowered.events.Cause;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.flowpowered.api.Platform;
import com.flowpowered.api.Spout;
import com.flowpowered.api.component.BlockComponentOwner;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.geo.LoadOption;
import com.flowpowered.api.geo.cuboid.Block;
import com.flowpowered.api.geo.cuboid.reference.ChunkReference;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.material.BlockMaterial;
import com.flowpowered.api.material.Material;
import com.flowpowered.api.material.block.BlockFace;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;

public class SpoutBlock implements Block {
	private final int x, y, z;
	private final WeakReference<? extends SpoutWorld> world;
	private final ChunkReference chunk;

	public SpoutBlock(SpoutWorld world, int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
        this.world = new WeakReference<>(world);
        this.chunk = new ChunkReference(new Point(getWorld(), this.x, this.y, this.z));
	}

	@Override
	public Point getPosition() {
		return new Point(getWorld(), this.x + 0.5f, this.y + 0.5f, this.z + 0.5f);
	}

	@Override
	public SpoutChunk getChunk() {
        return (SpoutChunk) this.chunk.refresh(LoadOption.LOAD_GEN);
	}

	@Override
	public SpoutWorld getWorld() {
		SpoutWorld world = this.world.get();
		if (world == null) {
			throw new IllegalStateException("The world has been unloaded!");
		}
		return world;
	}

	@Override
	public int getX() {
		return this.x;
	}

	@Override
	public int getY() {
		return this.y;
	}

	@Override
	public int getZ() {
		return this.z;
	}

	@Override
	public Block translate(BlockFace offset, int distance) {
		return this.translate(offset.getOffset().mul(distance));
	}

	@Override
	public Block translate(BlockFace offset) {
		if (offset == null) {
			return null;
		}
		return this.translate(offset.getOffset());
	}

	@Override
	public Block translate(Vector3f offset) {
		return this.translate((int) offset.getX(), (int) offset.getY(), (int) offset.getZ());
	}

	@Override
	public Block translate(Vector3i offset) {
		return this.translate(offset.getX(), offset.getY(), offset.getZ());
	}

	@Override
	public Block translate(int dx, int dy, int dz) {
		return new SpoutBlock(getWorld(), this.x + dx, this.y + dy, this.z + dz);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		} else if (other != null && other instanceof Block) {
			Block b = (Block) other;
			return b.getWorld() == this.getWorld() && b.getX() == this.getX() && b.getY() == this.getY() && b.getZ() == this.getZ();
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(getWorld()).append(getX()).append(getY()).append(getZ()).toHashCode();
	}

	@Override
	public String toString() {
		return StringUtil.toNamedString(this, this.world.get(), this.x, this.y, this.z);
	}

	@Override
	public boolean setMaterial(BlockMaterial material, int data, Cause<?> cause) {
		// TODO once stable, remove this
		if (Spout.getPlatform() != Platform.SERVER) {
			throw new UnsupportedOperationException("Temporary lockdown of setMaterial. Server only!");
		}
		return this.getChunk().setBlockMaterial(x, y, z, material, (short) data, cause);
	}

	@Override
	public boolean setMaterial(BlockMaterial material, int data) {
		return setMaterial(material, data, null);
	}

	@Override
	public SpoutBlock setData(BlockMaterial data) {
		return this.setData(data.getData());
	}

	@Override
	public SpoutBlock setData(int data) {
		return setData(data, null);
	}

	@Override
	public SpoutBlock setData(int data, Cause<?> cause) {
		this.getChunk().setBlockData(this.x, this.y, this.z, (short) data, cause);
		return this;
	}

	@Override
	public SpoutBlock addData(int data) {
		this.getChunk().addBlockData(this.x, this.y, this.z, (short) data, null);
		return this;
	}

	@Override
	public short getBlockData() {
		return this.getChunk().getBlockData(this.x, this.y, this.z);
	}

	@Override
	public short setDataBits(int bits) {
		return this.getChunk().setBlockDataBits(this.x, this.y, this.z, bits, null);
	}

	@Override
	public short setDataBits(int bits, boolean set) {
		return this.getChunk().setBlockDataBits(this.x, this.y, this.z, bits, set, null);
	}

	@Override
	public short clearDataBits(int bits) {
		return this.getChunk().clearBlockDataBits(this.x, this.y, this.z, bits, null);
	}

	@Override
	public int getDataField(int bits) {
		return this.getChunk().getBlockDataField(this.x, this.y, this.z, bits);
	}

	@Override
	public boolean isDataBitSet(int bits) {
		return this.getChunk().isBlockDataBitSet(this.x, this.y, this.z, bits);
	}

	@Override
	public int setDataField(int bits, int value) {
		return this.getChunk().setBlockDataField(this.x, this.y, this.z, bits, value, null);
	}

	@Override
	public int addDataField(int bits, int value) {
		return this.getChunk().addBlockDataField(this.x, this.y, this.z, bits, value, null);
	}

	@Override
	public SpoutRegion getRegion() {
		return (SpoutRegion) this.getChunk().getRegion();
	}

	@Override
	public BlockMaterial getMaterial() {
		return this.getChunk().getBlockMaterial(this.x, this.y, this.z);
	}

	@Override
	public boolean setMaterial(BlockMaterial material) {
		return this.setMaterial(material, material.getData());
	}

	@Override
	public boolean setMaterial(BlockMaterial material, Cause<?> cause) {
		return this.setMaterial(material, material.getData(), cause);
	}

	@Override
	public boolean isMaterial(Material... materials) {
		return getMaterial().isMaterial(materials);
	}

	@Override
	public <T extends Component> T add(Class<T> type) {
		return getChunk().getBlockComponentOwner(x, y, z, true).add(type);
	}

	@Override
	public <T extends Component> T detach(Class<? extends Component> type) {
		BlockComponentOwner owner = getChunk().getBlockComponentOwner(x, y, z, false);
		if (owner != null) {
			return owner.detach(type);
		}
		return null;
	}

	@Override
	public Collection<Component> values() {
		return getChunk().getBlockComponentOwner(x, y, z, true).values();
	}

	@Override
	public ManagedMap getData() {
		BlockComponentOwner owner = getChunk().getBlockComponentOwner(x, y, z, false);
		if (owner == null) {
			throw new IllegalStateException("The datatable is only available on blocks who have a BlockComponentOwner (blocks with components added)");
		}
		return owner.getData();
	}

	@Override
	public <T extends Component> T get(Class<T> type) {
		return getChunk().getBlockComponentOwner(x, y, z, true).get(type);
	}

	@Override
	public <T> T getType(Class<T> type) {
		return getChunk().getBlockComponentOwner(x, y, z, true).getType(type);
	}

	@Override
	public <T extends Component> T getExact(Class<T> type) {
		return getChunk().getBlockComponentOwner(x, y, z, true).getExact(type);
	}

	@Override
	public <T extends Component> Collection<T> getAll(Class<T> type) {
		return getChunk().getBlockComponentOwner(x, y, z, true).getAll(type);
	}

	@Override
	public <T> Collection<T> getAllOfType(Class<T> type) {
		return getChunk().getBlockComponentOwner(x, y, z, true).getAllOfType(type);
	}
}
