package com.flowpowered.engine.registry;

/**
 * Temporary Object as placeholder to show working of new Entity Setup
 * <p/>
 * The Entity Type will be used to combine all necessary information like
 * behaviour, texture, model etc into one object
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class TempEntityType {

	private String name;

	public TempEntityType(String name) {
		this.name = name.toUpperCase();
	}

	public String toString() {
		return name;
	}
}
