package com.flowpowered.api.material.block;

/**
 * Please change this
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public interface TempClientPlugin extends TempPlugin {

	/**
	 * Called when the plugin is enabled
	 */
	public void onClientEnable();

	/**
	 * Called when the plugins is disabled
	 */
	public void onClientDisable();

	/**
	 * Called when the client is reloaded
	 */
	public void onClientReload();

	/**
	 * Called when the plugin is initially loaded
	 */
	public void onClientLoad();

	/**
	 * Called when the client is unloaded
	 */
	public void onClientUnload();
}
