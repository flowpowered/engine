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

import java.util.Collection;
import java.util.UUID;

public interface WorldManager {

    /**
     * Searches for an actively loaded world that exactly matches the given name. <br/> <br/> The implementation is identical to iterating over {@link #getWorlds()} and checking for a world that matches
     * {@link World#getName()}. <br/> <br/>
     *
     * Worlds are added to the list immediately, but removed at the end of a tick.
     *
     * @param name of the world to search for
     * @return {@link World} if found, else null
     */
    public World getWorld(String name);

    /**
     * Searches for an actively loaded world that exactly matches the given name. <br/> <br/> If searching for the exact name, this method will iterate and check for exact matches. <br/> <br/> Otherwise,
     * this method will iterate over over all worlds and find the closest match to the given name, by comparing the length of other player names that start with the given parameter. <br/> <br/>
     *
     * Worlds are added to the list immediately, but removed at the end of a tick.
     *
     * @param name of the world to search for
     * @param exact Whether to use exact lookup
     * @return world if found, else null
     */
    public World getWorld(String name, boolean exact);

    /**
     * Searches for actively loaded worlds that matches the given name. <br/> <br/> The implementation is identical to iterating over {@link #getWorlds()} and checking for a world that matches {@link
     * World#getName()} <br/> <br/>
     *
     * Worlds are added to the list immediately, but removed at the end of a tick.
     *
     * @param name of the world to search for, or part of it
     * @return a collection of worlds that matched the name
     */
    public Collection<World> matchWorld(String name);

    /**
     * Searches for an actively loaded world has the given {@link UUID}. <br/> <br/> The implementation is identical to iterating over {@link #getWorlds()} and checking for a world that matches {@link
     * World#getUID()}. <br/> <br/>
     *
     * Worlds are added to the list immediately, but removed at the end of a tick.
     *
     * @param uid of the world to search for
     * @return {@link World} if found, else null
     */
    public World getWorld(UUID uid);

    /**
     * Gets a List of all currently loaded worlds <br/> Worlds are added to the list immediately, but removed at the end of a tick.
     *
     * @return {@link Collection} of actively loaded worlds
     */
    public Collection<World> getWorlds();
}
