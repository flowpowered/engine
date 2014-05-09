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
package com.flowpowered.api.material;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.flowpowered.api.Flow;
import com.flowpowered.api.Server;
import com.flowpowered.api.material.block.BlockFullState;
import com.flowpowered.api.util.SyncedStringMap;
import com.flowpowered.commons.store.BinaryFileStore;
import com.flowpowered.commons.store.MemoryStore;
import com.flowpowered.math.GenericMath;

/**
 * Handles all registered materials on the server statically.
 */
public abstract class MaterialRegistry {

	private final static ConcurrentHashMap<String, BaseMaterial> nameLookup = new ConcurrentHashMap<>(1000);
	private final static int MAX_SIZE = 1 << 16;
	@SuppressWarnings("unchecked")
	private final static AtomicReference<BaseMaterial[]>[] materialLookup = new AtomicReference[MAX_SIZE];
	private static boolean setup = false;
	private static SyncedStringMap materialRegistry;
	private final static BaseMaterial[] NULL_BASIC_MATERIAL_ARRAY = new BaseMaterial[]{null};

	static {
		for (int i = 0; i < materialLookup.length; i++) {
			materialLookup[i] = new AtomicReference<>();
			materialLookup[i].set(NULL_BASIC_MATERIAL_ARRAY);
		}
	}

	/**
	 * Sets up the material registry for its first use. May not be called more than once.<br/> This attempts to load the materials.dat file from the 'worlds' directory into memory.<br/>
	 * <p/>
	 * Can throw an {@link IllegalStateException} if the material registry has already been setup.
	 *
	 * @return StringToUniqueIntegerMap of registered materials
	 */
	public static SyncedStringMap setupRegistry() {
		if (setup) {
			throw new IllegalStateException("Can not setup material registry twice!");
		}
		if (Flow.getPlatform().isServer()) {
			setupServer();
		} else {
			setupClient();
		}

		setup = true;
		return materialRegistry;
	}

	private static void setupServer() {
		File serverItemMap = new File(((Server) Flow.getEngine()).getWorldManager().getWorldFolder(), "materials.dat");
		BinaryFileStore store = new BinaryFileStore(serverItemMap);
		materialRegistry = SyncedStringMap.create(null, store, 1, Short.MAX_VALUE, BaseMaterial.class.getName());
		if (serverItemMap.exists()) {
			store.load();
		}
	}

	private static void setupClient() {
		materialRegistry = SyncedStringMap.create(null, new MemoryStore<Integer>(), 1, Short.MAX_VALUE, BaseMaterial.class.getName());
	}

	/**
	 * Registers the baseMaterial in the baseMaterial lookup service
	 *
	 * @param baseMaterial to register
	 *
	 * @return id of the baseMaterial registered
	 */
	protected static int register(BaseMaterial baseMaterial) {
		if (baseMaterial.isSubMaterial()) {
			baseMaterial.getParentMaterial().registerSubMaterial(baseMaterial);
			nameLookup.put(formatName(baseMaterial.getDisplayName()), baseMaterial);
			return baseMaterial.getParentMaterial().getId();
		} else {
			int id = materialRegistry.register(baseMaterial.getName());
			BaseMaterial[] subArray = new BaseMaterial[]{baseMaterial};
			if (!materialLookup[id].compareAndSet(NULL_BASIC_MATERIAL_ARRAY, subArray)) {
				throw new IllegalArgumentException(materialLookup[id].get() + " is already mapped to id: " + baseMaterial.getId() + "!");
			}

			nameLookup.put(formatName(baseMaterial.getDisplayName()), baseMaterial);
			return id;
		}
	}

	protected static AtomicReference<BaseMaterial[]> getSubMaterialReference(short id) {
		return materialLookup[id];
	}

	/**
	 * Registers the baseMaterial in the baseMaterial lookup service
	 *
	 * @param baseMaterial to register
	 *
	 * @return id of the baseMaterial registered.
	 */
	protected static int register(BaseMaterial baseMaterial, int id) {
		materialRegistry.register(baseMaterial.getName(), id);
		BaseMaterial[] subArray = new BaseMaterial[]{baseMaterial};
		if (!materialLookup[id].compareAndSet(NULL_BASIC_MATERIAL_ARRAY, subArray)) {
			throw new IllegalArgumentException(materialLookup[id].get()[0] + " is already mapped to id: " + baseMaterial.getId() + "!");
		}

		nameLookup.put(formatName(baseMaterial.getName()), baseMaterial);
		return id;
	}

	/**
	 * Gets the material from the given id
	 *
	 * @param id to get
	 *
	 * @return material or null if none found
	 */
	public static BaseMaterial get(short id) {
		if (id < 0 || id >= materialLookup.length) {
			return null;
		}
		return materialLookup[id].get()[0];
	}

	/**
	 * Gets the material from the given id and data
	 *
	 * @param id   to get
	 * @param data to get
	 *
	 * @return material or null if none found
	 */
	public static BaseMaterial get(short id, short data) {
		if (id < 0 || id >= materialLookup.length) {
			return null;
		}
		BaseMaterial[] parent = materialLookup[id].get();
		if (parent[0] == null) {
			return null;
		}

		data &= parent[0].getDataMask();
		return materialLookup[id].get()[data];
	}

	/**
	 * Gets the material for the given BlockFullState
	 *
	 * @param state the full state of the block
	 *
	 * @return BaseMaterial of the BlockFullState
	 */
	public static BaseMaterial get(BlockFullState state) {
		return get(state.getPacked());
	}

	/**
	 * Gets the material for the given packed full state
	 *
	 * @param state the full state of the block
	 *
	 * @return BaseMaterial of the id
	 */
	public static BlockBaseMaterial get(int packedState) {
		short id = BlockFullState.getId(packedState);
		if (id < 0 || id >= materialLookup.length) {
			return null;
		}
		BaseMaterial[] baseMaterial = materialLookup[id].get();
		if (baseMaterial[0] == null) {
			return null;
		}
		return (BlockBaseMaterial) baseMaterial[BlockFullState.getData(packedState) & (baseMaterial[0].getDataMask())];
	}

	/**
	 * Returns all current materials in the game
	 *
	 * @return an array of all materials
	 */
	public static BaseMaterial[] values() {
		//TODO: This is wrong, need to count # of registered materials
		HashSet<BaseMaterial> set = new HashSet<>(1000);
		for (int i = 0; i < materialLookup.length; i++) {
			if (materialLookup[i].get() != null) {
				set.add(materialLookup[i].get()[0]);
			}
		}
		return set.toArray(new BaseMaterial[0]);
	}

	/**
	 * Gets the associated material with its name. Case-insensitive.
	 *
	 * @param name to lookup
	 *
	 * @return material, or null if none found
	 */
	public static BaseMaterial get(String name) {
		return nameLookup.get(formatName(name));
	}

	/**
	 * Returns a human legible material name from the full material.
	 * <p/>
	 * This will strip any '_' and replace with spaces, strip out extra whitespace, and lowercase the material name.
	 *
	 * @return human legible name of the material.
	 */
	private static String formatName(String matName) {
		return matName.trim().replaceAll(" ", "_").toLowerCase();
	}

	/**
	 * Gets the minimum data mask required to account for all sub-materials of the material
	 *
	 * @param m the material
	 *
	 * @return the minimum data mask
	 */
	public static short getMinimumDatamask(BaseMaterial m) {
		BaseMaterial root = m;
		while (root.isSubMaterial()) {
			root = m.getParentMaterial();
		}

		if (root.getData() != 0) {
			throw new IllegalStateException("Root materials must have data set to zero");
		}
		BaseMaterial[] subBaseMaterials = root.getSubMaterials();

		short minimumMask = 0;

		for (BaseMaterial sm : subBaseMaterials) {
			minimumMask |= sm.getData() & 0xFFFF;
		}

		if (m.hasLSBDataMask()) {
			minimumMask = (short) (GenericMath.roundUpPow2(minimumMask + 1) - 1);
		}

		return minimumMask;
	}
}
