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
