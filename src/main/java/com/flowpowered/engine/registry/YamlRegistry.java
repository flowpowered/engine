package com.flowpowered.engine.registry;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry object which is using the yaml standard to save data.
 * Todo make abstract to support other ways of saving / loading
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class YamlRegistry {

	private Map<Integer, String> idNameMap;
	private Map<String, Integer> nameIdMap;

	public YamlRegistry() {
		this(500);
	}

	public YamlRegistry(Integer number) {
		idNameMap = new HashMap<>(number);
		nameIdMap = new HashMap<>(number);
	}

	public String addIdName(Integer id, String name) {
		nameIdMap.put(name, id);
		return idNameMap.put(id, name);
	}

	public Integer addNameId(String name, Integer id) {
		idNameMap.put(id, name);
		return nameIdMap.put(name, id);
	}

	public Integer getIdByName(String name) {
		return nameIdMap.get(name);
	}

	public String getNameById(Integer id) {
		return idNameMap.get(id);
	}

	public String removeId(Integer id) {
		String name = idNameMap.get(id);
		nameIdMap.remove(name);
		return idNameMap.remove(id);
	}

	public Integer removeName(String name) {
		Integer id = nameIdMap.get(name);
		idNameMap.remove(id);
		return nameIdMap.remove(name);
	}

	public boolean containsKeyId(Integer id) {
		return idNameMap.containsKey(id);
	}

	public boolean containsKeyName(String name) {
		return nameIdMap.containsKey(name);
	}

	public boolean save(String file) {
		// Todo implement yaml save

		return false;
	}

	public boolean load(String file) {
		// Todo implement yaml load

		return false;
	}

	public boolean checkIfRegistryStorageExists(String file) {
		return new File(file + ".yml").exists();
	}
}

