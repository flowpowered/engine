package com.flowpowered.engine.registry;

import java.util.LinkedList;

import com.flowpowered.api.material.BlockBaseMaterial;

import static com.flowpowered.engine.registry.BlockAttributes.BASE_MATERIAL;
import static com.flowpowered.engine.registry.BlockAttributes.CUSTOM_MATERIAL;
import static com.flowpowered.engine.registry.BlockAttributes.ID;
import static com.flowpowered.engine.registry.BlockAttributes.NAME;

/**
 * BlockMaterial which contains the default game based {@link BlockBaseMaterial} and any other custom {@link BlockBaseMaterial}
 * TODO: add logging for FINE
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class BlockMaterial extends GenericRegistryObject<BlockAttributes> {

	BlockMaterial(final Class attributeGroup) {
		super(BlockAttributes.class);
	}

	static BlockMaterial createBlockMaterial(BlockBaseMaterial blockBaseMaterial) {
		return createBlockMaterial(blockBaseMaterial, null);
	}

	static BlockMaterial createBlockMaterial(BlockBaseMaterial blockBaseMaterial, BlockBaseMaterial customBlockBaseMaterial) {
		if (blockBaseMaterial == null) {
			if (customBlockBaseMaterial == null) {
				throw new IllegalArgumentException("Both materials can't be null!");
			} else {
				blockBaseMaterial = customBlockBaseMaterial;
			}
		} else {
			if (customBlockBaseMaterial == null) {
				customBlockBaseMaterial = blockBaseMaterial;
			}
		}
		BlockMaterial blockMaterial = new BlockMaterial(BlockAttributes.class);
		LinkedList<BlockBaseMaterial> customBlockBaseMaterialList = new LinkedList<>();
		customBlockBaseMaterialList.add(customBlockBaseMaterial);
		blockMaterial.set(ID, -1);
		blockMaterial.set(CUSTOM_MATERIAL, customBlockBaseMaterialList);
		blockMaterial.set(BASE_MATERIAL, blockBaseMaterial);
		blockMaterial.set(NAME, blockBaseMaterial.getName());
		return blockMaterial;
	}

	public BlockBaseMaterial getBlockBaseMaterial() {
		return this.get(BASE_MATERIAL);
	}

	public BlockBaseMaterial getCustomBlockBaseMaterial() {
		return this.get(CUSTOM_MATERIAL).getLast();
	}

	public LinkedList<BlockBaseMaterial> getCustomBlockBaseMaterials() {
		return this.get(CUSTOM_MATERIAL);
	}

	public boolean addCustomBlockBaseMaterial(BlockBaseMaterial blockBaseMaterial) {
		LinkedList<BlockBaseMaterial> customBlockBaseMaterialList = this.getCustomBlockBaseMaterials();
		Boolean success = customBlockBaseMaterialList.add(blockBaseMaterial);
		if (success) {
			this.set(CUSTOM_MATERIAL, customBlockBaseMaterialList);
		}
		return success;
	}

	public boolean removeCustomBlockBaseMaterial(BlockBaseMaterial blockBaseMaterial) {
		Boolean success = false;
		if (blockBaseMaterial != this.getBlockBaseMaterial()) {
			LinkedList<BlockBaseMaterial> customBlockBaseMaterialList = this.getCustomBlockBaseMaterials();
			success = customBlockBaseMaterialList.remove(blockBaseMaterial);
			if (success) {
				this.set(CUSTOM_MATERIAL, customBlockBaseMaterialList);
			}
		}
		return success;
	}

	public boolean revertToLastCustomBlockBaseMaterial() {
		LinkedList<BlockBaseMaterial> customBlockBaseMaterialList = this.getCustomBlockBaseMaterials();
		Boolean success = (customBlockBaseMaterialList.removeLast() != null);
		if (success) {
			this.set(CUSTOM_MATERIAL, customBlockBaseMaterialList);
		}
		return success;
	}

	Integer getID() {
		return this.get(ID);
	}

	void setID(Integer id) {
		this.set(ID, id);
	}

	public String getName() {
		return this.get(NAME);
	}
}
