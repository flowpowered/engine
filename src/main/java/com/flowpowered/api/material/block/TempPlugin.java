package com.flowpowered.api.material.block;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.flowpowered.api.Engine;
import com.flowpowered.api.generator.WorldGenerator;
import com.flowpowered.commons.Named;
import com.flowpowered.engine.registry.BlockMaterialRegistry;

import net.java.games.util.plugins.PluginLoader;
import org.slf4j.Logger;

/**
 * Please change this
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public interface TempPlugin extends Named {

	/**
	 * Returns true if the plugins is enabled
	 *
	 * @return enabled
	 */
	public boolean isEnabled();

	/**
	 * Changes the enabled state of the plugin This should only be called by the
	 * plugin's loader
	 *
	 * @param enabled
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Returns the plugin's loader
	 *
	 * @return loader
	 */
	public PluginLoader getPluginLoader();

	/**
	 * Returns the plugin's logger
	 *
	 * @return logger
	 */
	public Logger getLogger();

	/**
	 * Returns the plugin's description
	 * @return description
	 */
	// public PluginDescriptionFile getDescription();

	/**
	 * Returns the engine object
	 *
	 * @return engine
	 */
	public Engine getEngine();

	/**
	 * Gets the suitable generator for the world and generator name.
	 *
	 * @param world     name to generate
	 * @param generator name
	 *
	 * @return world generator
	 */
	public WorldGenerator getWorldGenerator(String world, String generator);

	/**
	 * Returns the plugin's data folder
	 *
	 * @return
	 */
	public File getDataFolder();

	/**
	 * Returns a File that is the plugin's jar file.
	 *
	 * @return
	 */
	public File getFile();

	/**
	 * Returns a resource from the plugin's archive
	 *
	 * @param path The path of the resource to get
	 *
	 * @return The resource's input stream, or null if none could be found or the implementation does not support this method
	 */
	public InputStream getResource(String path);

	/**
	 * Extracts a resource returned by {@link #getResource(String)} to the given path
	 *
	 * @param path        The path to get the resource at
	 * @param destination The destination file
	 *
	 * @throws IOException When the resource could not be found or the copying failed
	 */
	public void extractResource(String path, File destination) throws IOException;

	/**
	 * Allows plugins to load external libraries into the JVM
	 *
	 * @param file that is the library
	 */
	public void loadLibrary(File file);

	/**
	 * @return the plugins dictionary
	 */
	// public PluginDictionary getDictionary();

	/**
	 * @return the plugins BlockMaterialRegistry
	 */
	public BlockMaterialRegistry getBlockMaterialRegistry();

	/**
	 * @return the plugins ItemMaterialRegistry
	 */
	//public ItemMaterialRegistry getItemMaterialRegistry();

	/**
	 * @return the plugins EntitiyMaterialRegistry
	 */
	//public EntityMaterialRegistry getEntityMaterialRegistry();
}
