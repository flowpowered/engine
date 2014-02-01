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
package com.flowpowered.engine.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.spout.nbt.ByteArrayTag;
import org.spout.nbt.ByteTag;
import org.spout.nbt.CompoundMap;
import org.spout.nbt.CompoundTag;
import org.spout.nbt.LongTag;
import org.spout.nbt.StringTag;
import org.spout.nbt.stream.NBTInputStream;
import org.spout.nbt.stream.NBTOutputStream;
import org.spout.nbt.util.NBTMapper;

import com.flowpowered.api.Flow;
import com.flowpowered.api.Server;
import com.flowpowered.api.generator.WorldGenerator;
import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.io.nbt.TransformTag;
import com.flowpowered.api.io.nbt.UUIDTag;
import com.flowpowered.commons.StringToUniqueIntegerMap;
import com.flowpowered.commons.datatable.SerializableMap;
import com.flowpowered.commons.sanitation.SafeCast;
import com.flowpowered.commons.store.BinaryFileStore;
import com.flowpowered.engine.FlowEngine;
import com.flowpowered.engine.geo.world.FlowServerWorld;

public class WorldFiles {
    public static final byte WORLD_VERSION = 1;

    public static <E extends FlowEngine & Server> FlowServerWorld loadWorld(E engine, WorldGenerator generator, String worldName) {
        File worldDir = new File(FlowFileSystem.WORLDS_DIRECTORY, worldName);
        worldDir.mkdirs();
        File worldFile = new File(worldDir, "world.dat");

        FlowServerWorld world = null;

        File itemMapFile = new File(worldDir, "materials.dat");
        BinaryFileStore itemStore = new BinaryFileStore(itemMapFile);
        if (itemMapFile.exists()) {
            itemStore.load();
        }

        //StringToUniqueIntegerMap itemMap = new StringToUniqueIntegerMap(engine.getEngineItemMap(), itemStore, 0, Short.MAX_VALUE, worldName + "ItemMap");
        StringToUniqueIntegerMap itemMap = null;

        /*
         File lightingMapFile = new File(worldDir, "lighting.dat");
         BinaryFileStore lightingStore = new BinaryFileStore(lightingMapFile);
         if (lightingMapFile.exists()) {
         lightingStore.load();
         }
         StringToUniqueIntegerMap lightingMap = new StringToUniqueIntegerMap(engine.getEngineLightingMap(), lightingStore, 0, Short.MAX_VALUE, worldName + "LightingMap");
         */

        try {
            InputStream is = new FileInputStream(worldFile);
            NBTInputStream ns = new NBTInputStream(is, false);
            CompoundMap map;
            try {
                CompoundTag tag = (CompoundTag) ns.readTag();
                map = tag.getValue();
            } finally {
                try {
                    ns.close();
                } catch (IOException e) {
                    Flow.info("Cannot close world file");
                }
            }
            Flow.info("Loading world [{}]", worldName);
            world = loadWorldImpl(engine, worldName, map, generator, itemMap);
        } catch (FileNotFoundException ioe) {
            Flow.info("Creating new world named [{}]", worldName);

            world = new FlowServerWorld(engine, worldName, generator);
            world.save();
        } catch (IOException ioe) {
            Flow.severe("Error reading file for world " + worldName, ioe);
        }
        return world;
    }

    private static FlowServerWorld loadWorldImpl(FlowEngine engine, String name, CompoundMap map, WorldGenerator fallbackGenerator, StringToUniqueIntegerMap itemMap) {
        byte version = SafeCast.toByte(NBTMapper.toTagValue(map.get("version")), (byte) -1);
        if (version > WORLD_VERSION) {
            Flow.severe("World version " + version + " exceeds maximum allowed value of " + WORLD_VERSION);
            return null;
        } else if (version < WORLD_VERSION) {
            Flow.severe("Outdated World version " + version);
            return null;
        }

        String generatorName = SafeCast.toString(NBTMapper.toTagValue(map.get("generator")), null);
        Long seed = SafeCast.toLong(NBTMapper.toTagValue(map.get("seed")), 0);
        byte[] extraData = SafeCast.toByteArray(NBTMapper.toTagValue(map.get("extra_data")), null);
        Long age = SafeCast.toLong(NBTMapper.toTagValue(map.get("age")), 0);
        UUID uuid = UUIDTag.getValue(map.get("uuid"));

        WorldGenerator generator = findGenerator(generatorName, fallbackGenerator);

        FlowServerWorld world = new FlowServerWorld(engine, name, uuid, age, generator, seed);

        Transform t = TransformTag.getValue(world, map.get("spawn_position"));

        world.setSpawnPoint(t);

        SerializableMap dataMap = world.getData();
        dataMap.clear();
        try {
            dataMap.deserialize(extraData);
        } catch (IOException e) {
            Flow.severe("Could not deserialize datatable for world: " + name, e);
        }

        return world;
    }

    private static WorldGenerator findGenerator(String wanted, WorldGenerator given) {
        // TODO: lookup class name
        if (!wanted.equals(given.getClass().getName())) {
            Flow.severe("World was saved last with the generator: " + wanted + " but is being loaded with: " + given.getClass().getName() + " THIS MAY CAUSE WORLD CORRUPTION!");
        }
        return given;
    }

    public static void saveWorld(FlowServerWorld world) {

        File worldDir = new File(FlowFileSystem.WORLDS_DIRECTORY, world.getName());

        worldDir.mkdirs();

        File worldFile = new File(worldDir, "world.dat");

        //world.getItemMap().save();

        //world.getLightingMap().save();

        CompoundMap map = saveWorldImpl(world);

        NBTOutputStream ns = null;
        try {
            OutputStream is = new FileOutputStream(worldFile);
            ns = new NBTOutputStream(is, false);
            ns.writeTag(new CompoundTag("world_" + world.getName(), map));
        } catch (IOException ioe) {
            Flow.severe("Error writing file for world " + world.getName());
        } finally {
            if (ns != null) {
                try {
                    ns.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static CompoundMap saveWorldImpl(FlowServerWorld world) {
        CompoundMap map = new CompoundMap();

        map.put(new ByteTag("version", WORLD_VERSION));
        map.put(new StringTag("generator", world.getGenerator().getClass().getName()));
        map.put(new LongTag("seed", world.getSeed()));
        map.put(new ByteArrayTag("extra_data", world.getData().serialize()));
        map.put(new LongTag("age", world.getAge()));
        map.put(new UUIDTag("uuid", world.getUID()));
        map.put(new TransformTag("spawn_position", world.getSpawnPoint()));

        return map;
    }
}
