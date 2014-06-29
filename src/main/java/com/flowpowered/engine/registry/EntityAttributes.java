package com.flowpowered.engine.registry;

/**
 * Please change this
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public interface EntityAttributes extends AttributeGroup {

	public static final Attribute<EntityAttributes, String> NAME = Attribute.get("NAME");
	public static final Attribute<EntityAttributes, TempEntityType> ENTITY_TYPE = Attribute.get("ENTITY_TYPE");
}
