package com.flowpowered.api.material.block;

/**
 * Please change this
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public interface TempServerPlugin extends TempPlugin {

	/**
	 * Called when the plugin is enabled
	 */
	public void onServerEnable();

	/**
	 * Called when the plugins is disabled
	 */
	public void onServerDisable();

	/**
	 * Called when the server is reloaded
	 */
	public void onServerReload();

	/**
	 * Called when the plugin is initially loaded
	 */
	public void onServerLoad();

	/**
	 * Called when the server is unloaded
	 */
	public void onServerUnload();
}
