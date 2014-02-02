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
package com.flowpowered.api.geo;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.flowpowered.api.generator.WorldGenerator;

public interface ServerWorldManager extends WorldManager {

    /**
     * Gets the default world generator for this game. Specific generators can be specified when loading new worlds.
     *
     * @return default world generator.
     */
    public WorldGenerator getDefaultGenerator();

    /**
     * Loads a {@link World} with the given name and {@link WorldGenerator}<br/> If the world doesn't exist on disk, it creates it.<br/> <br/> if the world is already loaded, this functions the same as
     * {@link #getWorld(String)}
     *
     * @param name Name of the world
     * @param generator World Generator
     * @return {@link World} loaded or created.
     */
    public World loadWorld(String name, WorldGenerator generator);

    /**
     * Unloads this world from memory. <br/> <br/> <b>Note: </b>Worlds can not be unloaded if players are currently on them.
     *
     * @param name of the world to unload
     * @param save whether or not to save the world state to file
     * @return true if the world was unloaded, false if not
     */
    public boolean unloadWorld(String name, boolean save);

    /**
     * Unloads this world from memory. <br/> <br/> <b>Note: </b>Worlds can not be unloaded if players are currently on them.
     *
     * @param world to unload
     * @param save whether or not to save the world state to file
     * @return true if the world was unloaded, false if not
     */
    public boolean unloadWorld(ServerWorld world, boolean save);

    /**
     * Initiates a save of the server state, including configuration files. <br/> <br/> It will save the state of the world, if specificed, and the state of players, if specified.
     *
     * @param worlds true to save the state of all active worlds
     * @param players true to save the state of all active players
     */
    public void save(boolean worlds, boolean players);

    /**
     * Gets the world folders which match the world name.
     *
     * @param worldName to match the world folders with
     * @return the world folders that match the world name
     */
    public Collection<File> matchWorldFolder(String worldName);

    /**
     * Gets all the individual world folders where world data is stored. <br/> <br/> This includes offline worlds.
     *
     * @return a list of available world folders
     */
    public List<File> getWorldFolders();

    /**
     * Gets the folder that contains the world save data. <br/> <br/> If the folder is unusued, the file path will be '.'
     *
     * @return world folder
     */
    public File getWorldFolder();
}
