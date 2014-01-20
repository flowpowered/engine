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
package com.flowpowered.api.geo;

import java.util.List;
import java.util.UUID;

import com.flowpowered.commons.Named;
import com.flowpowered.commons.datatable.ManagedMap;
import com.flowpowered.events.Cause;

import com.flowpowered.api.Engine;
import com.flowpowered.api.component.Component;
import com.flowpowered.api.component.ComponentOwner;
import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.entity.EntityPrefab;
import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.scheduler.TaskManager;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.math.vector.Vector3f;

/**
 * Represents a World.
 */
public interface World extends AreaRegionAccess, Named, ComponentOwner {
	/**
	 * Gets the name of the world
	 *
	 * @return the name of the world
	 */
	@Override
	public String getName();

	/**
	 * Gets the age of the world in ms. This count cannot be modified, and increments on every tick.
	 *
	 * @return the world's age in ms
	 */
	public long getAge();

	/**
	 * Gets the UID representing the world. With extremely high probability the UID is unique to each world.
	 *
	 * @return the name of the world
	 */
	public UUID getUID();

	/**
	 * Gets the entity with the matching unique id <p> Performs a search on each region for the entity, stopping when it is found, or after all the worlds have been searched upon failure.
	 *
	 * @param uid to search and match
	 * @return entity that matched the uid, or null if none was found
	 */
	public Entity getEntity(UUID uid);

	/**
	 * Spawns an {@link Entity} at the {@link Point} blueprinted with the {@link EntityPrefab} provided. <p>
     * The {@link LoadOption} parameter is used to tell Spout if it should load or not load the region for the point provided.
     * If the region is not loaded currently, or it isn't loaded as specified by the {@link LoadOption}, then this may return null and the entity may not be spawned.
	 *
	 * @param point The area in space to spawn
	 * @param option Whether to not load or not load the region if it is not currently loaded
	 * @param prefab The blueprint
	 * @return The spawned entity at the point with the prefab applied
	 */
	public Entity spawnEntity(Vector3f point, LoadOption option, EntityPrefab prefab);

	/**
     * @see World#spawnEntity(com.flowpowered.math.vector.Vector3f, com.flowpowered.api.geo.LoadOption, com.flowpowered.api.entity.EntityPrefab)
     *
	 * @param point The area in space to spawn
	 * @param option Whether to not load, load, or load and create the chunk
	 * @param classes The classes to attach
	 * @return The spawned entity at the point with the components attached
	 */
	public Entity spawnEntity(Vector3f point, LoadOption option, Class<? extends Component>... classes);

	/**
     * @see World#spawnEntity(com.flowpowered.math.vector.Vector3f, com.flowpowered.api.geo.LoadOption, com.flowpowered.api.entity.EntityPrefab)
	 *
	 * @param points The areas in space to spawn
	 * @param option Whether to not load, load, or load and create the chunk
	 * @param classes The classes to attach
	 * @return The spawned entities at the points with the components attached
	 */
	public Entity[] spawnEntities(Vector3f[] points, LoadOption option, Class<? extends Component>... classes);

	/**
	 * Gets the engine associated with this world
	 *
	 * @return the engine
	 */
	public Engine getEngine();

	/**
	 * Gets all entities with the specified type.
	 *
	 * @return A collection of entities with the specified type.
	 */
	public List<Entity> getAll();

	/**
	 * Gets an entity by its id.
	 *
	 * @param id The id.
	 * @return The entity, or {@code null} if it could not be found.
	 */
	public Entity getEntity(int id);

	/**
	 * Gets the task manager responsible for parallel region tasks.<br> <br> All tasks are submitted to all loaded regions at the start of the next tick.<br> <br> Repeating tasks are also submitted to
	 * all new regions when they are created.<br> Repeated tasks are NOT guaranteed to happen in the same tick for all regions, as each task is submitted individually to each region.<br> <br> This task
	 * manager does not support async tasks. <br> If the Runnable for the task is a ParallelRunnable, then a new instance of the Runnable will be created for each region.
	 *
	 * @return the parallel task manager for the engine
	 */
	public TaskManager getParallelTaskManager();

	/**
	 * Gets the TaskManager associated with this world
	 *
	 * @return task manager
	 */
	public abstract TaskManager getTaskManager();

	/**
	 * Gets a list of nearby entities of the point, inside of the range
	 *
	 * @param position of the center
	 * @param ignore Entity to ignore
	 * @param range to look for
	 * @return the list of nearby entities (or empty if none)
	 */
	public List<Entity> getNearbyEntities(Point position, Entity ignore, int range);

	/**
	 * Gets a set of nearby players to the point, inside of the range
	 *
	 * @param position of the center
	 * @param range to look for
	 * @return A set of nearby Players
	 */
	public List<Entity> getNearbyEntities(Point position, int range);

	/**
	 * Gets a set of nearby players to the entity, inside of the range
	 *
	 * @param entity marking the center and which is ignored
	 * @param range to look for
	 * @return A set of nearby Players
	 */
	public List<Entity> getNearbyEntities(Entity entity, int range);

	/**
	 * Gets the absolute closest player from the specified point within a specified range.
	 *
	 * @param position to search from
	 * @param ignore to ignore while searching
	 * @param range to search
	 * @return nearest player
	 */
	public Entity getNearestEntity(Point position, Entity ignore, int range);

	/**
	 * Gets the absolute closest player from the specified point within a specified range.
	 *
	 * @param position center of search
	 * @param range to search
	 * @return nearest player
	 */
	public Entity getNearestEntity(Point position, int range);

	/**
	 * Gets the absolute closest player from the specified point within a specified range.
	 *
	 * @param entity to search from
	 * @param range to search
	 * @return nearest player
	 */
	public Entity getNearestEntity(Entity entity, int range);

	/**
	 * Gets a set of nearby players to the point, inside of the range. The search will ignore the specified entity.
	 *
	 * @param position of the center
	 * @param ignore Entity to ignore
	 * @param range to look for
	 * @return A set of nearby Players
	 */
	public List<Player> getNearbyPlayers(Point position, Player ignore, int range);

	/**
	 * Gets a set of nearby players to the point, inside of the range
	 *
	 * @param position of the center
	 * @param range to look for
	 * @return A set of nearby Players
	 */
	public List<Player> getNearbyPlayers(Point position, int range);

	/**
	 * Gets a set of nearby players to the entity, inside of the range
	 *
	 * @param entity marking the center and which is ignored
	 * @param range to look for
	 * @return A set of nearby Players
	 */
	public List<Player> getNearbyPlayers(Entity entity, int range);

	/**
	 * Gets the absolute closest player from the specified point within a specified range.
	 *
	 * @param position to search from
	 * @param ignore to ignore while searching
	 * @param range to search
	 * @return nearest player
	 */
	public Player getNearestPlayer(Point position, Player ignore, int range);

	/**
	 * Gets the absolute closest player from the specified point within a specified range.
	 *
	 * @param position center of search
	 * @param range to search
	 * @return nearest player
	 */
	public Player getNearestPlayer(Point position, int range);

	/**
	 * Gets the absolute closest player from the specified point within a specified range.
	 *
	 * @param entity to search from
	 * @param range to search
	 * @return nearest player
	 */
	public Player getNearestPlayer(Entity entity, int range);

	/**
	 * Atomically sets the cuboid volume to the values inside of the cuboid buffer.
	 *
	 * @param cause that is setting the cuboid volume
	 */
	@Override
	public void setCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause);

	/**
	 * Atomically sets the cuboid volume to the values inside of the cuboid buffer with the base located at the given coords
	 *
	 * @param cause that is setting the cuboid volume
	 */
	@Override
	public void setCuboid(int x, int y, int z, CuboidBlockMaterialBuffer buffer, Cause<?> cause);

	/**
	 * Atomically gets the cuboid volume with the base located at the given coords of the given size.<br> <br> Note: The block at the base coordinate is inside the
	 *
	 * @param bx base x-coordinate
	 * @param by base y-coordinate
	 * @param bz base z-coordinate
	 * @param sx size x-coordinate
	 * @param sy size y-coordinate
	 * @param sz size z-coordinate
	 */
	@Override
	public CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz);

	/**
	 * Atomically gets the cuboid volume with the base located at the given coords and the size of the given buffer.<br> <br> Note: The block at the base coordinate is inside the
	 *
	 * @param bx base x-coordinate
	 * @param by base y-coordinate
	 * @param bz base z-coordinate
	 */
	@Override
	public void getCuboid(int bx, int by, int bz, CuboidBlockMaterialBuffer buffer);

	/**
	 * Atomically gets the cuboid volume contained within the given buffer
	 *
	 * @param buffer the buffer
	 */
	@Override
	public void getCuboid(CuboidBlockMaterialBuffer buffer);

	/**
	 * Gets the {@link ManagedMap} which a world always has.
	 *
	 * @return ManagedMap
	 */
	@Override
	public ManagedMap getData();

	/**
	 * Gets a set of all players on active on this world
	 *
	 * @return all players on this world
	 */
	public List<Player> getPlayers();
}
