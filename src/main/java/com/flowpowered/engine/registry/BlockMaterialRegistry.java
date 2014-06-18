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
	String blockRegistryFileName = plugin.getDataFolder() + "blockregistry";
	private YamlRegistry yamlRegistry = new YamlRegistry(500);
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
		checkIfRegistryIsInitialized();
		if (!blockBaseMaterial.getClass().getName().contains(gameClassPath)) {
			throw new IllegalArgumentException("BlockBaseMaterial must be from classes of the registering plugin: " + gameClass.getName() + " !");
		}
		String blockMaterialName = blockBaseMaterial.getName();
		BlockMaterial blockMaterial;
		if (yamlRegistry.containsKeyName(blockMaterialName)) {
			Integer id = yamlRegistry.getIdByName(blockMaterialName);
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

	private void checkIfRegistryIsInitialized() {
		if (!initialized) {
			if (!initializeRegistry()) {
				throw new RegistryNotInitializedException("BlockRegistry");
			}
		}
	}

	public boolean addCustomBlockBaseMaterial(String blockMaterialName, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfRegistryIsInitialized();
		checkIfBlockMaterialNameExists(blockMaterialName);
		return addCustomBlockBaseMaterial(yamlRegistry.getIdByName(blockMaterialName), customBlockBaseMaterial);
	}

	public boolean addCustomBlockBaseMaterial(Integer id, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfRegistryIsInitialized();
		checkIfBlockMaterialIdExists(id);
		BlockMaterial blockMaterial = idRegistryMap.get(id);
		return blockMaterial.addCustomBlockBaseMaterial(customBlockBaseMaterial);
	}

	public boolean removeCustomBlockBaseMaterial(String blockMaterialName, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfRegistryIsInitialized();
		checkIfBlockMaterialNameExists(blockMaterialName);
		return removeCustomBlockBaseMaterial(yamlRegistry.getIdByName(blockMaterialName), customBlockBaseMaterial);
	}

	private void checkIfBlockMaterialNameExists(final String blockMaterialName) {
		if (!yamlRegistry.containsKeyName(blockMaterialName)) {
			throw new IllegalArgumentException("The BlockMaterial with the name " + blockMaterialName + " doesn't exist!");
		}
	}

	public boolean removeCustomBlockBaseMaterial(Integer id, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfRegistryIsInitialized();
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
		checkIfRegistryIsInitialized();
		checkIfBlockMaterialNameExists(blockMaterialName);
		return revertToLastCustomBlockBaseMaterial(yamlRegistry.getIdByName(blockMaterialName));
	}

	public BlockMaterial getBlockMaterialByName(String blockMaterialName) {
		checkIfRegistryIsInitialized();
		checkIfBlockMaterialNameExists(blockMaterialName);
		Integer id = yamlRegistry.getIdByName(blockMaterialName);
		return getBlockMaterialByID(id);
	}

	public BlockMaterial getBlockMaterialByID(Integer id) {
		checkIfRegistryIsInitialized();
		checkIfBlockMaterialIdExists(id);
		return idRegistryMap.get(id);
	}

	public boolean revertToLastCustomBlockBaseMaterial(Integer id) {
		checkIfRegistryIsInitialized();
		checkIfBlockMaterialIdExists(id);
		BlockMaterial blockMaterial = idRegistryMap.get(id);
		return blockMaterial.revertToLastCustomBlockBaseMaterial();
	}

	private BlockMaterial addBlockMaterialToRegistry(Integer id, BlockBaseMaterial blockBaseMaterial, BlockBaseMaterial customBlockBaseMaterial) {
		checkIfRegistryIsInitialized();
		String blockMaterialName = blockBaseMaterial.getName();
		yamlRegistry.addIdName(id, blockMaterialName);
		BlockMaterial blockMaterial = BlockMaterial.createBlockMaterial(blockBaseMaterial, customBlockBaseMaterial);
		idRegistryMap.put(id, blockMaterial);
		blockMaterial.setID(id);
		return blockMaterial;
	}

	private boolean initializeRegistry() {
		if (initialized) {
			Flow.fine("This BlockRegistry is already initialized, doing nothing!");
			return true;
		}
		if (Flow.getPlatform().isServer()) {
			if (yamlRegistry.checkIfRegistryStorageExists(blockRegistryFileName)) {
				if (!loadRegistry()) {
					throw new RegistryIOException("BlockRegistry", "loaded");
				}
			}
		}
		initialized = true;
		return initialized;
	}

	public boolean loadRegistry() {
		if (Flow.getPlatform().isClient()) {
			Flow.debug("Won't be loading registry on client platform!");
			return true;
		}
		return yamlRegistry.load(blockRegistryFileName);
	}

	public boolean saveRegistry() {
		if (Flow.getPlatform().isClient()) {
			Flow.debug("Won't be saving registry on client platform!");
			return true;
		}
		checkIfRegistryIsInitialized();
		return yamlRegistry.save(blockRegistryFileName);
	}
}
