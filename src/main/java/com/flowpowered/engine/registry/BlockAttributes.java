package com.flowpowered.engine.registry;

import java.util.LinkedList;

import com.flowpowered.api.material.BlockBaseMaterial;

/**
 * Please change this
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public interface BlockAttributes extends AttributeGroup {

	public static final Attribute<BlockAttributes, String> NAME = Attribute.get("NAME");
	public static final Attribute<BlockAttributes, Integer> ID = Attribute.get("ID");
	public static final Attribute<BlockAttributes, LinkedList<BlockBaseMaterial>> CUSTOM_MATERIAL = Attribute.get("CUSTOM_MATERIAL");
	public static final Attribute<BlockAttributes, BlockBaseMaterial> BASE_MATERIAL = Attribute.get("BASE_MATERIAL");
}
