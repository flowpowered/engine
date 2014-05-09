package com.flowpowered.engine.registry;

import com.flowpowered.api.material.BaseMaterial;
import com.flowpowered.api.material.BlockBaseMaterial;

/**
 * This is the connection between the engine and the original baseMaterial.
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class MaterialMapper {

	private int id;
	private BlockBaseMaterial baseMaterial;
	private BlockBaseMaterial fallbackBaseMaterial;

	protected MaterialMapper(int id, BaseMaterial baseMaterial, BaseMaterial fallbackBaseMaterial) {
		this.id = id;
		if (baseMaterial == null) {
			if (fallbackBaseMaterial == null) {
				throw new IllegalArgumentException("Both materials can't be null!");
			} else {
				baseMaterial = fallbackBaseMaterial;
			}
		} else {
			if (fallbackBaseMaterial == null) {
				fallbackBaseMaterial = baseMaterial;
			}
		}
	}

	public int getId() {
		return id;
	}

	public BaseMaterial getBaseMaterial() {
		return baseMaterial;
	}

	public BaseMaterial getFallbackBaseMaterial() {
		return fallbackBaseMaterial;
	}
}
