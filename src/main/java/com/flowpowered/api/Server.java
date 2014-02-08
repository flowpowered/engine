/*
 * This file is part of Flow Engine, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.api;

import java.util.Collection;

import com.flowpowered.api.player.Player;
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
    Collection<Player> getOnlinePlayers();

    /**
     * Gets the maximum number of players this game can host, or -1 if infinite
     *
     * @return max players
     */
    int getMaxPlayers();

    /**
     * Broadcasts the given message to all players
     *
     * The implementation of broadcast is identical to iterating over {@link #getOnlinePlayers()} and invoking {@link Player#sendMessage(String)} for each player.
     *
     * @param message to send
     */
    void broadcastMessage(String message);

    /**
     * Broadcasts the given message to all players
     *
     * The implementation of broadcast is identical to calling a {@link com.flowpowered.api.event.server.permissions.PermissionGetAllWithNodeEvent} event, iterating over each element in getReceivers, invoking
     * {@link com.flowpowered.api.command.CommandSource#sendMessage(String)} for each CommandSource.
     *
     * @param permission the permission needed to receive the broadcast
     * @param message to send
     */
    void broadcastMessage(String permission, String message);

    /**
     * Gets the {@link Player} by the given username. <br/> <br/> If searching for the exact name, this method will iterate and check for exact matches. <br/> <br/> Otherwise, this method will iterate
     * over over all players and find the closest match to the given name, by comparing the length of other player names that start with the given parameter. <br/> <br/> This method is case-insensitive.
     *
     * @param name to look up
     * @param exact Whether to use exact lookup
     * @return Player if found, else null
     */
    Player getPlayer(String name, boolean exact);

    @Override
    ServerWorldManager getWorldManager();
}
