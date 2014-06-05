package com.flowpowered.engine.registry;

import java.util.HashMap;
import java.util.Map;

import com.flowpowered.api.Flow;
import com.flowpowered.api.material.BlockBaseMaterial;
import com.flowpowered.api.material.block.TempPlugin;

/**
 * Registry Class which handles everything concerning BlockMaterial interaction.
 * TODO: create abstract base class to support Item and Entity Registry
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class BlockMaterialRegistry {

	/**
	 * Class of the instancing Plugin.
	 * This is required to make sure that only objects from the instancing game can be used as fallback objects.
	 */
	private Class gameClass;
	private TempPlugin plugin;
	private String gameClassPath;
	private Map<Integer, String> idNameMap = new HashMap<>(500);
	private Map<String, Integer> nameIdMap = new HashMap<>(500);
	private Map<Integer, BlockMaterial> idRegistryMap = new HashMap<>(500);
	private Integer numberOfMaterials = 0;
	private boolean initialized = false;

	public BlockMaterialRegistry(TempPlugin game) {
		gameClass = game.getClass();
		gameClassPath = gameClass.getPackage().getName();
		this.plugin = game;
	}

	public BlockMaterial addBlockMaterial(BlockBaseMaterial blockBaseMaterial) {
		return addBlockMaterial(blockBaseMaterial, blockBaseMaterial);
	}

	public BlockMaterial addBlockMaterial(BlockBaseMaterial blockBaseMaterial, BlockBaseMaterial customBlockBaseMaterial) {
		if (!blockBaseMaterial.getClass().getName().contains(gameClassPath)) {
			throw new IllegalArgumentException("BlockBaseMaterial must be from classes of the registering plugin: " + gameClass.getName() + " !");
		}
		String blockMaterialName = blockBaseMaterial.getName();
		BlockMaterial blockMaterial;
		if (nameIdMap.containsKey(blockMaterialName)) {
			Integer id = nameIdMap.get(blockMaterialName);
			if (idRegistryMap.containsKey(id)) {
				blockMaterial = idRegistryMap.get(id);
				Flow.log("BlockMaterial with the Name " + blockMaterialName + " already exists!");
				Flow.fine("Returning original BlockMaterial");
				if (blockMaterial.getCustomBlockBaseMaterial().equals(customBlockBaseMaterial) || (customBlockBaseMaterial.getClass().getName().contains(gameClassPath))) {
					Flow.fine("without changes");
				} else {
					blockMaterial.addCustomBlockBaseMaterial(customBlockBaseMaterial);
				}
			} else {
				blockMaterial = addBlockMaterialToRegistry(id, blockBaseMaterial, customBlockBaseMaterial);
			}
		} else {
			numberOfMaterials++;
			blockMaterial = addBlockMaterialToRegistry(numberOfMaterials, blockBaseMaterial, customBlockBaseMaterial);
		}
		return blockMaterial;
	}

	public boolean addCustomBlockBaseMaterial(String blockMaterialName, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfBlockMaterialNameExists(blockMaterialName);
		return addCustomBlockBaseMaterial(nameIdMap.get(blockMaterialName), customBlockBaseMaterial);
	}

	public boolean addCustomBlockBaseMaterial(Integer id, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfBlockMaterialIdExists(id);
		BlockMaterial blockMaterial = idRegistryMap.get(id);
		return blockMaterial.addCustomBlockBaseMaterial(customBlockBaseMaterial);
	}

	public boolean removeCustomBlockBaseMaterial(String blockMaterialName, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfBlockMaterialNameExists(blockMaterialName);
		return removeCustomBlockBaseMaterial(nameIdMap.get(blockMaterialName), customBlockBaseMaterial);
	}

	private void checkIfBlockMaterialNameExists(final String blockMaterialName) {
		if (!nameIdMap.containsKey(blockMaterialName)) {
			throw new IllegalArgumentException("The BlockMaterial with the name " + blockMaterialName + " doesn't exist!");
		}
	}

	public boolean removeCustomBlockBaseMaterial(Integer id, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfBlockMaterialIdExists(id);
		BlockMaterial blockMaterial = idRegistryMap.get(id);
		return blockMaterial.removeCustomBlockBaseMaterial(customBlockBaseMaterial);
	}

	private void checkIfBlockMaterialIdExists(final Integer id) {
		if (!idRegistryMap.containsKey(id)) {
			throw new IllegalArgumentException("The BlockMaterial with the ID " + id + " doesn't exist!");
		}
	}

	public boolean revertToLastCustomBlockBaseMaterial(String blockMaterialName) {
		checkIfBlockMaterialNameExists(blockMaterialName);
		return revertToLastCustomBlockBaseMaterial(nameIdMap.get(blockMaterialName));
	}

	public BlockMaterial getBlockMaterialByName(String blockMaterialName) {
		checkIfBlockMaterialNameExists(blockMaterialName);
		Integer id = nameIdMap.get(blockMaterialName);
		return getBlockMaterialByID(id);
	}

	public BlockMaterial getBlockMaterialByID(Integer id) {
		checkIfBlockMaterialIdExists(id);
		return idRegistryMap.get(id);
	}

	public boolean revertToLastCustomBlockBaseMaterial(Integer id) {
		checkIfBlockMaterialIdExists(id);
		BlockMaterial blockMaterial = idRegistryMap.get(id);
		return blockMaterial.revertToLastCustomBlockBaseMaterial();
	}

	private BlockMaterial addBlockMaterialToRegistry(Integer id, BlockBaseMaterial blockBaseMaterial, BlockBaseMaterial customBlockBaseMaterial) {
		String blockMaterialName = blockBaseMaterial.getName();
		idNameMap.put(id, blockMaterialName);
		nameIdMap.put(blockMaterialName, id);
		BlockMaterial blockMaterial = BlockMaterial.createBlockMaterial(blockBaseMaterial, customBlockBaseMaterial);
		idRegistryMap.put(id, blockMaterial);
		blockMaterial.setID(id);
		return blockMaterial;
	}

	private boolean initializeRegistry() {
		// TODO: add reading of initial registry if saved file found
		return false;
	}

	public boolean loadRegistry() {
		// TODO: add loading of registry from file
		return false;
	}

	public boolean saveRegistry() {
		// TODO: add saving of registry to file
		return false;
	}
}
