/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package com.flowpowered.api;

import com.flowpowered.commons.Named;
import com.flowpowered.events.EventManager;
import com.flowpowered.filesystem.FileSystem;
import com.flowpowered.api.geo.WorldManager;
import com.flowpowered.api.scheduler.Scheduler;

/**
 * Represents the core of an implementation of an engine (powers a game).
 */
public interface Engine extends Named {

	/**
	 * Gets the version.
	 *
	 * @return build version
	 */
	public String getVersion();

    public Platform getPlatform();

	/**
	 * Ends this engine instance safely. All worlds, players, and configuration data is saved, and all threads are ended cleanly.<br/> <br/> Players will be sent a default disconnect message.
	 *
	 * @return true for for the first stop
	 */
	public boolean stop();

	/**
	 * Ends this engine instance safely. All worlds, players, and configuration data is saved, and all threads are ended cleanly. <br/> If any players are connected, will kick them with the given reason.
	 *
	 * @param reason for stopping the game instance
	 * @return true for for the first stop
	 */
	public boolean stop(String reason);

	/**
	 * Returns true if the game is running in debug mode <br/> <br/> To start debug mode, start Spout with -debug
	 *
	 * @return true if server is started with the -debug flag, false if not
	 */
	public boolean debugMode();

    public Scheduler getScheduler();

	/**
	 * Gets an abstract representation of the engine Filesystem. <p> The Filesystem handles the loading of all resources. <p> On the client, loading a resource will load the resource from the harddrive.
	 * On the server, it will notify all clients to load the resource, as well as provide a representation of that resource.
     * 
     * @return the filesystem instance
     */
	public FileSystem getFileSystem();

	/**
	 * Returns the game's {@link EventManager} Event listener registration and calling is handled through this.
	 *
	 * @return Our EventManager instance
	 */
	public EventManager getEventManager();

    public WorldManager getWorldManager();
}
