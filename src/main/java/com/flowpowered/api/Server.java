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

import java.util.Collection;

import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.ServerWorldManager;

/**
 * Represents the server-specific implementation.
 */
public interface Server extends Engine {
	/**
	 * Gets all players currently online
	 *
	 * @return array of all active players
	 */
	public Collection<Player> getOnlinePlayers();

	/**
	 * Gets the maximum number of players this game can host, or -1 if infinite
	 *
	 * @return max players
	 */
	public int getMaxPlayers();

	/**
	 * Broadcasts the given message to all players
	 *
	 * The implementation of broadcast is identical to iterating over {@link #getOnlinePlayers()} and invoking {@link Player#sendMessage(String)} for each player.
	 *
	 * @param message to send
	 */
	public void broadcastMessage(String message);

	/**
	 * Broadcasts the given message to all players
	 *
	 * The implementation of broadcast is identical to calling a {@link com.flowpowered.api.event.server.permissions.PermissionGetAllWithNodeEvent} event, iterating over each element in getReceivers, invoking
	 * {@link com.flowpowered.api.command.CommandSource#sendMessage(String)} for each CommandSource.
	 *
     * @param permission the permission needed to receive the broadcast
     * @param message to send
	 */
	public void broadcastMessage(String permission, String message);

	/**
	 * Gets the {@link Player} by the given username. <br/> <br/> If searching for the exact name, this method will iterate and check for exact matches. <br/> <br/> Otherwise, this method will iterate
	 * over over all players and find the closest match to the given name, by comparing the length of other player names that start with the given parameter. <br/> <br/> This method is case-insensitive.
	 *
	 * @param name to look up
	 * @param exact Whether to use exact lookup
	 * @return Player if found, else null
	 */
	public Player getPlayer(String name, boolean exact);

    @Override
    public ServerWorldManager getWorldManager();
}
