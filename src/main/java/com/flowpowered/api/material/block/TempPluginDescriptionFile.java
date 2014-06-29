package com.flowpowered.api.material.block;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.flowpowered.api.Platform;

import org.spout.cereal.config.ConfigurationException;
import org.spout.cereal.config.ConfigurationNode;
import org.spout.cereal.config.ConfigurationNodeSource;
import org.spout.cereal.config.serialization.Serialization;
import org.spout.cereal.config.yaml.YamlConfiguration;

/**
 * Please change this
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class TempPluginDescriptionFile {

	public static final List<String> RESTRICTED_NAMES = Collections.unmodifiableList(Arrays.asList(
			"org.spout",
			"org.getspout",
			"org.spoutcraft",
			"in.spout"));
	private final HashMap<String, String> data = new HashMap<String, String>();
	private String name;
	private String version;
	private String description;
	private List<String> authors = new ArrayList<String>();
	private String website;
	private boolean reload;
	private Platform platform;
	//private LoadOrder load;
	private String main;
	private List<String> depends;
	private List<String> softdepends;
	private String fullname;
	private Locale codedLocale = Locale.ENGLISH;

	public TempPluginDescriptionFile(String name, String version, String main, Platform platform) {
		this.name = name;
		this.version = version;
		this.main = main;
		this.platform = platform;
		fullname = name + " v" + version;
	}

	public TempPluginDescriptionFile(InputStream stream) //throws InvalidDescriptionFileException {
	{
		YamlConfiguration yaml = new YamlConfiguration(stream);
		try {
			yaml.load();
		} catch (ConfigurationException e) {
			//throw new InvalidDescriptionFileException(e);
		}
		load(yaml);
	}

	public TempPluginDescriptionFile(Reader reader) //throws InvalidDescriptionFileException {
	{
		YamlConfiguration yaml = new YamlConfiguration(reader);
		try {
			yaml.load();
		} catch (ConfigurationException e) {
			//throw new InvalidDescriptionFileException(e);
		}
		load(yaml);
	}

	public TempPluginDescriptionFile(String raw) //throws InvalidDescriptionFileException {
	{
		YamlConfiguration yaml = new YamlConfiguration(raw);
		try {
			yaml.load();
		} catch (ConfigurationException e) {
			//throw new InvalidDescriptionFileException(e);
		}
		load(yaml);
	}

	@SuppressWarnings("unchecked")
	private void load(Map<?, ?> map) //throws InvalidDescriptionFileException {
	{
		name = getEntry("name", String.class, map);
		if (!name.matches("^[A-Za-z0-9 _.-]+$")) {
			//throw new InvalidDescriptionFileException("The field 'name' in properties.yml contains invalid characters.");
		}
		if (name.toLowerCase().contains("spout")) {
			//throw new InvalidDescriptionFileException("The plugin '" + name + "' has Spout in the name. This is not allowed.");
		}

		main = getEntry("main", String.class, map);
		if (!isOfficialPlugin(main)) {
			for (String namespace : RESTRICTED_NAMES) {
				if (main.startsWith(namespace)) {
					//throw new InvalidDescriptionFileException("The use of the namespace '" + namespace + "' is not permitted.");
				}
			}
		}

		version = getEntry("version", String.class, map);
		platform = getEntry("platform", Platform.class, map);
		fullname = name + " v" + version;

		if (map.containsKey("author")) {
			authors.add(getEntry("author", String.class, map));
		}

		if (map.containsKey("authors")) {
			authors.addAll(getEntry("authors", List.class, map));
		}

		if (map.containsKey("depends")) {
			depends = getEntry("depends", List.class, map);
		}

		if (map.containsKey("softdepends")) {
			softdepends = getEntry("softdepends", List.class, map);
		}

		if (map.containsKey("description")) {
			description = getEntry("description", String.class, map);
		}

		if (map.containsKey("load")) {
			//load = getEntry("load", LoadOrder.class, map);
		}

		if (map.containsKey("reload")) {
			reload = getEntry("reload", Boolean.class, map);
		}

		if (map.containsKey("website")) {
			website = getEntry("website", String.class, map);
		}

		if (map.containsKey("codedlocale")) {
			Locale[] locales = Locale.getAvailableLocales();
			for (Locale l : locales) {
				if (l.getLanguage().equals((new Locale((String) map.get("codedlocale"))).getLanguage())) {
					codedLocale = l;
				}
			}
		}
		if (map.containsKey("data")) {
			Map<?, ?> data = getEntry("data", Map.class, map);
			for (Map.Entry<?, ?> entry : data.entrySet()) {
				String key = entry.getKey().toString();
				String value = entry.getValue().toString();
				this.data.put(key, value);
			}
		}
	}

	private void load(YamlConfiguration yaml) //throws InvalidDescriptionFileException {
	{
		name = getEntry("name", String.class, yaml);
		if (!name.matches("^[A-Za-z0-9 _.-]+$")) {
			//throw new InvalidDescriptionFileException("The field 'name' in properties.yml contains invalid characters.");
		}
		if (name.toLowerCase().contains("spout")) {
			//throw new InvalidDescriptionFileException("The plugin '" + name + "' has Spout in the name. This is not allowed.");
		}

		main = getEntry("main", String.class, yaml);
		if (!isOfficialPlugin(main)) {
			for (String namespace : RESTRICTED_NAMES) {
				if (main.startsWith(namespace)) {
					//throw new InvalidDescriptionFileException("The use of the namespace '" + namespace + "' is not permitted.");
				}
			}
		}

		version = getEntry("version", String.class, yaml);
		platform = getEntry("platform", Platform.class, yaml);
		fullname = name + " v" + version;

		if (yaml.hasChild("author")) {
			authors.add(getEntry("author", String.class, yaml));
		}

		if (yaml.hasChild("authors")) {
			authors.addAll(getEntry("authors", List.class, yaml));
		}

		if (yaml.hasChild("depends")) {
			depends = getEntry("depends", List.class, yaml);
		}

		if (yaml.hasChild("softdepends")) {
			softdepends = getEntry("softdepends", List.class, yaml);
		}

		if (yaml.hasChild("description")) {
			description = getEntry("description", String.class, yaml);
		}

		if (yaml.hasChild("load")) {
			//load = getEntry("load", LoadOrder.class, yaml);
		}

		if (yaml.hasChild("reload")) {
			reload = getEntry("reload", Boolean.class, yaml);
		}

		if (yaml.hasChild("website")) {
			website = getEntry("website", String.class, yaml);
		}

		if (yaml.hasChild("codedlocale")) {
			Locale[] locales = Locale.getAvailableLocales();
			for (Locale l : locales) {
				if (l.getLanguage().equals((new Locale(yaml.getChild("codedlocale").getString())).getLanguage())) {
					codedLocale = l;
				}
			}
		}
		if (yaml.hasChild("data")) {
			Map<String, ConfigurationNode> data = yaml.getChild("data").getChildren();
			for (Map.Entry<String, ConfigurationNode> entry : data.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue().getString();
				this.data.put(key, value);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getEntry(Object key, Class<T> type, Map<?, ?> values) //throws InvalidDescriptionFileException {
	{
		Object value = values.get(key);
		if (value == null) {
			//throw new InvalidDescriptionFileException("The field '" + key + "' is not present in the properties.yml!");
		}

		return (T) Serialization.deserialize(type, value);
	}

	private <T> T getEntry(String key, Class<T> type, ConfigurationNodeSource src) //throws InvalidDescriptionFileException {
	{
		T value = src.getChild(key).getTypedValue(type);
		if (value == null) {
			//throw new InvalidDescriptionFileException("The field '" + key + "' is not present in the properties.yml!");
		}
		return value;
	}

	/**
	 * Returns true if the plugin is an Official Spout Plugin
	 *
	 * @param namespace The plugin's main class namespace
	 *
	 * @return true if an official plugin
	 */
	private boolean isOfficialPlugin(String namespace) {
		return (namespace.equalsIgnoreCase("org.spout.vanilla.plugin.VanillaPlugin")
				|| namespace.equalsIgnoreCase("org.spout.bridge.VanillaBridgePlugin")
				|| namespace.equalsIgnoreCase("org.spout.infobjects.InfObjectsPlugin")
				|| namespace.startsWith("org.spout.droplet"));
	}

	/**
	 * Returns the plugin's name
	 *
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the plugin's version
	 *
	 * @return version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the plugin's description
	 *
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the plugin's authors
	 *
	 * @return authors
	 */
	public List<String> getAuthors() {
		return authors;
	}

	/**
	 * Returns the plugin's website
	 *
	 * @return website
	 */
	public String getWebsite() {
		return website;
	}

	/**
	 * Returns false if the plugin wants to be exempt from a reload
	 *
	 * @return reload
	 */
	public boolean allowsReload() {
		return reload;
	}

	/**
	 * Returns the plugin's platform
	 *
	 * @return platform
	 */
	public Platform getPlatform() {
		return platform;
	}

	/**
	 * Returns the plugin's load order
	 * @return load
	 */
	//public LoadOrder getLoad() {
	//	return load;
	//}

	/**
	 * Returns the path the plugins main class
	 *
	 * @return main
	 */
	public String getMain() {
		return main;
	}

	/**
	 * Returns the plugin's dependencies
	 *
	 * @return depends
	 */
	public List<String> getDepends() {
		return depends;
	}

	/**
	 * Returns the plugin's soft dependencies
	 *
	 * @return softdepends
	 */
	public List<String> getSoftDepends() {
		return softdepends;
	}

	/**
	 * Returns the plugin's fullname The fullname is formatted as follows:
	 * [name] v[version]
	 *
	 * @return The full name of the plugin
	 */
	public String getFullName() {
		return fullname;
	}

	/**
	 * Returns the locale the strings in the plugin are coded in.
	 * Will be read from the plugins properties.yml from the field "codedlocale"
	 *
	 * @return the locale the plugin is coded in
	 */
	public Locale getCodedLocale() {
		return codedLocale;
	}

	public String getData(String key) {
		return data.get(key);
	}
}
