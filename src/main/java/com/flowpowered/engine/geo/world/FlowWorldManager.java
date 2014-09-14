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
package com.flowpowered.engine.geo.world;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.flowpowered.api.generator.EmptyWorldGenerator;
import com.flowpowered.api.generator.WorldGenerator;
import com.flowpowered.api.geo.ServerWorld;
import com.flowpowered.api.geo.ServerWorldManager;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldManager;
import com.flowpowered.commons.StringUtil;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.filesystem.FlowFileSystem;
import com.flowpowered.engine.filesystem.WorldFiles;
import com.google.common.collect.Lists;

public class FlowWorldManager implements WorldManager, ServerWorldManager {
    private static final WorldGenerator defaultGenerator = new EmptyWorldGenerator();
    protected final FlowEngine engine;
    protected final ConcurrentMap<String, FlowServerWorld> loadedWorlds;

    public FlowWorldManager(FlowEngine engine) {
        loadedWorlds = new ConcurrentHashMap<>();
        this.engine = engine;
    }

    public void addWorld(FlowServerWorld world) {
        loadedWorlds.put(world.getName(), world);
    }

    @Override
    public World getWorld(String name) {
        return getWorld(name, true);
    }

    @Override
    public World getWorld(String name, boolean exact) {
        if (exact) {
            FlowWorld world = loadedWorlds.get(name);
            if (world != null) {
                return world;
            }
            return loadedWorlds.get(name);
        } else {
            return StringUtil.getShortest(StringUtil.matchName(loadedWorlds.values(), name));
        }
    }

    @Override
    public Collection<World> matchWorld(String name) {
        return StringUtil.matchName(getWorlds(), name);
    }

    @Override
    public FlowWorld getWorld(UUID uid) {
        for (FlowWorld world : loadedWorlds.values()) {
            if (world.getUID().equals(uid)) {
                return world;
            }
        }
        return null;
    }

    @Override
    public Collection<World> getWorlds() {
        return loadedWorlds.values().stream().collect(Collectors.toList());
    }

    @Override
    public Collection<Path> matchWorldFolder(final String worldName) {
        try {
            return Lists.newArrayList(Files.newDirectoryStream(getWorldFolder(),
                    (Path entry) -> Files.isDirectory(entry) && Files.exists(entry.resolve("world.dat")) && StringUtil.startsWithIgnoreCase(entry.getFileName().toString(), worldName)));
        } catch (IOException ex) {
            engine.getLogger().error("Exception when matching world folder", ex);
            return Collections.emptyList();
        }
    }

    @Override
    public FlowServerWorld loadWorld(String name, WorldGenerator generator) {
        if (loadedWorlds.containsKey((name))) {
            return loadedWorlds.get(name);
        }

        if (generator == null) {
            generator = defaultGenerator;
        }

        FlowServerWorld world = WorldFiles.loadWorld(engine, generator, name);

        FlowServerWorld oldWorld = loadedWorlds.putIfAbsent(name, world);

        if (oldWorld != null) {
            return oldWorld;
        }

        world.getThread().start();
        //getEventManager().callDelayedEvent(new WorldLoadEvent(world));
        return world;
    }

    @Override
    public void save(boolean worlds, boolean players) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Path> getWorldFolders() {
        DirectoryStream<Path> stream;
        try {
            stream = Files.newDirectoryStream(getWorldFolder(), entry -> Files.isDirectory(entry) && Files.exists(entry.resolve("world.dat")));
        } catch (IOException ex) {
            engine.getLogger().error("Error when listing world folders.", ex);
            return Collections.emptyList();
        }
        return Lists.newArrayList(stream);
    }

    @Override
    public Path getWorldFolder() {
        return FlowFileSystem.WORLDS_DIRECTORY;
    }

    @Override
    public WorldGenerator getDefaultGenerator() {
        return defaultGenerator;
    }

    @Override
    public boolean unloadWorld(String name, boolean save) {
        return unloadWorld(loadedWorlds.get(name), save);
    }

    @Override
    public boolean unloadWorld(ServerWorld world, boolean save) {
        if (world == null) {
            return false;
        }

        FlowServerWorld w = (FlowServerWorld) world;
        boolean success = loadedWorlds.remove(world.getName(), w);
        if (success) {
            if (save) {
                //getEventManager().callDelayedEvent(new WorldUnloadEvent(world));
                w.unload(save);
            }
            w.getThread().stop();
            // Note: Worlds should not allow being saved twice and/or throw exceptions if accessed after unloading.
            // Also, should blank out as much internal world data as possible, in case plugins retain references to unloaded worlds.
        }
        return success;
    }

}
