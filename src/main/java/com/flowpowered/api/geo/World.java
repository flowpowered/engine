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
import com.flowpowered.api.player.Player;
import com.flowpowered.api.geo.discrete.Point;
import com.flowpowered.api.scheduler.TaskManager;
import com.flowpowered.api.util.UnloadSavable;
import com.flowpowered.api.util.cuboid.CuboidBlockMaterialBuffer;
import com.flowpowered.math.vector.Vector3f;

/**
 * Represents a World.
 */
public interface World extends AreaRegionAccess, Named, ComponentOwner, UnloadSavable {
    /**
     * Gets the name of the world
     *
     * @return the name of the world
     */
    @Override
    String getName();

    /**
     * Gets the age of the world in ms. This count cannot be modified, and increments on every tick.
     *
     * @return the world's age in ms
     */
    long getAge();

    /**
     * Gets the UID representing the world. With extremely high probability the UID is unique to each world.
     *
     * @return the name of the world
     */
    UUID getUID();

    /**
     * Gets the entity with the matching unique id <p> Performs a search on each region for the entity, stopping when it is found, or after all the worlds have been searched upon failure.
     *
     * @param uid to search and match
     * @return entity that matched the uid, or null if none was found
     */
    Entity getEntity(UUID uid);

    /**
     * Spawns an {@link Entity} at the {@link Point} blueprinted with the {@link EntityPrefab} provided. <p>
     * The {@link LoadOption} parameter is used to tell Flow if it should load or not load the region for the point provided.
     * If the region is not loaded currently, or it isn't loaded as specified by the {@link LoadOption}, then this may return null and the entity may not be spawned.
     *
     * @param point The area in space to spawn
     * @param option Whether to not load or not load the region if it is not currently loaded
     * @param prefab The blueprint
     * @return The spawned entity at the point with the prefab applied
     */
    Entity spawnEntity(Vector3f point, LoadOption option, EntityPrefab prefab);

    /**
     * @see World#spawnEntity(com.flowpowered.math.vector.Vector3f, com.flowpowered.api.geo.LoadOption, com.flowpowered.api.entity.EntityPrefab)
     *
     * @param point The area in space to spawn
     * @param option Whether to not load, load, or load and create the chunk
     * @param classes The classes to attach
     * @return The spawned entity at the point with the components attached
     */
    @SuppressWarnings("unchecked")
    Entity spawnEntity(Vector3f point, LoadOption option, Class<? extends Component>... classes);

    /**
     * @see World#spawnEntity(com.flowpowered.math.vector.Vector3f, com.flowpowered.api.geo.LoadOption, com.flowpowered.api.entity.EntityPrefab)
     *
     * @param points The areas in space to spawn
     * @param option Whether to not load, load, or load and create the chunk
     * @param classes The classes to attach
     * @return The spawned entities at the points with the components attached
     */
    @SuppressWarnings("unchecked")
    Entity[] spawnEntities(Vector3f[] points, LoadOption option, Class<? extends Component>... classes);

    /**
     * Gets the engine associated with this world
     *
     * @return the engine
     */
    Engine getEngine();

    /**
     * Gets all entities with the specified type.
     *
     * @return A collection of entities with the specified type.
     */
    List<Entity> getAll();

    /**
     * Gets an entity by its id.
     *
     * @param id The id.
     * @return The entity, or {@code null} if it could not be found.
     */
    Entity getEntity(int id);

    /**
     * Gets the TaskManager associated with this world
     *
     * @return task manager
     */
    abstract TaskManager getTaskManager();

    /**
     * Gets a list of nearby entities of the point, inside of the range
     *
     * @param position of the center
     * @param ignore Entity to ignore
     * @param range to look for
     * @return the list of nearby entities (or empty if none)
     */
    List<Entity> getNearbyEntities(Point position, Entity ignore, int range);

    /**
     * Gets a set of nearby players to the point, inside of the range
     *
     * @param position of the center
     * @param range to look for
     * @return A set of nearby Players
     */
    List<Entity> getNearbyEntities(Point position, int range);

    /**
     * Gets a set of nearby players to the entity, inside of the range
     *
     * @param entity marking the center and which is ignored
     * @param range to look for
     * @return A set of nearby Players
     */
    List<Entity> getNearbyEntities(Entity entity, int range);

    /**
     * Gets the absolute closest player from the specified point within a specified range.
     *
     * @param position to search from
     * @param ignore to ignore while searching
     * @param range to search
     * @return nearest player
     */
    Entity getNearestEntity(Point position, Entity ignore, int range);

    /**
     * Gets the absolute closest player from the specified point within a specified range.
     *
     * @param position center of search
     * @param range to search
     * @return nearest player
     */
    Entity getNearestEntity(Point position, int range);

    /**
     * Gets the absolute closest player from the specified point within a specified range.
     *
     * @param entity to search from
     * @param range to search
     * @return nearest player
     */
    Entity getNearestEntity(Entity entity, int range);

    /**
     * Gets a set of nearby players to the point, inside of the range. The search will ignore the specified entity.
     *
     * @param position of the center
     * @param ignore Entity to ignore
     * @param range to look for
     * @return A set of nearby Players
     */
    List<Player> getNearbyPlayers(Point position, Player ignore, int range);

    /**
     * Gets a set of nearby players to the point, inside of the range
     *
     * @param position of the center
     * @param range to look for
     * @return A set of nearby Players
     */
    List<Player> getNearbyPlayers(Point position, int range);

    /**
     * Gets a set of nearby players to the entity, inside of the range
     *
     * @param entity marking the center and which is ignored
     * @param range to look for
     * @return A set of nearby Players
     */
    List<Player> getNearbyPlayers(Entity entity, int range);

    /**
     * Gets the absolute closest player from the specified point within a specified range.
     *
     * @param position to search from
     * @param ignore to ignore while searching
     * @param range to search
     * @return nearest player
     */
    Player getNearestPlayer(Point position, Player ignore, int range);

    /**
     * Gets the absolute closest player from the specified point within a specified range.
     *
     * @param position center of search
     * @param range to search
     * @return nearest player
     */
    Player getNearestPlayer(Point position, int range);

    /**
     * Gets the absolute closest player from the specified point within a specified range.
     *
     * @param entity to search from
     * @param range to search
     * @return nearest player
     */
    Player getNearestPlayer(Entity entity, int range);

    /**
     * Atomically sets the cuboid volume to the values inside of the cuboid buffer.
     *
     * @param cause that is setting the cuboid volume
     */
    @Override
    void setCuboid(CuboidBlockMaterialBuffer buffer, Cause<?> cause);

    /**
     * Atomically sets the cuboid volume to the values inside of the cuboid buffer with the base located at the given coords
     *
     * @param cause that is setting the cuboid volume
     */
    @Override
    void setCuboid(int x, int y, int z, CuboidBlockMaterialBuffer buffer, Cause<?> cause);

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
    CuboidBlockMaterialBuffer getCuboid(int bx, int by, int bz, int sx, int sy, int sz);

    /**
     * Atomically gets the cuboid volume with the base located at the given coords and the size of the given buffer.<br> <br> Note: The block at the base coordinate is inside the
     *
     * @param bx base x-coordinate
     * @param by base y-coordinate
     * @param bz base z-coordinate
     */
    @Override
    void getCuboid(int bx, int by, int bz, CuboidBlockMaterialBuffer buffer);

    /**
     * Atomically gets the cuboid volume contained within the given buffer
     *
     * @param buffer the buffer
     */
    @Override
    void getCuboid(CuboidBlockMaterialBuffer buffer);

    /**
     * Gets the {@link ManagedMap} which a world always has.
     *
     * @return ManagedMap
     */
    @Override
    ManagedMap getData();

    /**
     * Gets a set of all players on active on this world
     *
     * @return all players on this world
     */
    List<Player> getPlayers();
}
