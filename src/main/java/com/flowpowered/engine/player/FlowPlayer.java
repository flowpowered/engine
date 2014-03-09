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
package com.flowpowered.engine.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.flowpowered.api.entity.Entity;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.discrete.TransformProvider;
import com.flowpowered.api.input.InputSnapshot;
import com.flowpowered.api.input.KeyboardEvent;
import com.flowpowered.api.input.MouseEvent;
import com.flowpowered.api.player.Player;
import com.flowpowered.api.player.PlayerNetwork;
import com.flowpowered.api.player.PlayerSnapshot;
import com.flowpowered.chat.ChatReceiver;
import com.flowpowered.commands.CommandException;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.network.FlowSession;
import com.flowpowered.permissions.PermissionDomain;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class FlowPlayer implements Player {
    protected final String name;
    protected final PlayerNetwork network;
    protected volatile TransformProvider transformProvider = TransformProvider.NullTransformProvider.INSTANCE;
    private volatile Map<ThreadGroup, List<InputSnapshot>> inputSnapshots = new HashMap<>();
    private volatile Cache<FlowWorld, ConcurrentLinkedQueue<InputSnapshot>> liveInput = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .weakKeys()
            .removalListener(new RemovalListener<FlowWorld, ConcurrentLinkedQueue<InputSnapshot>>() {
                @Override
                public void onRemoval(RemovalNotification<FlowWorld, ConcurrentLinkedQueue<InputSnapshot>> rn) {
                    network.getSession().getEngine().getLogger().warn("World '" + rn.getKey() + "' evicted from input cache for player '" + name + "' because: " + rn.getCause());
                }
            })
            .build();
    private volatile InputSnapshot lastLiveInput = new InputSnapshot();
    private final Object inputMutex = new Object();

    public FlowPlayer(FlowSession session, String name) {
        this.name = name;
        this.network = new PlayerNetwork(session);
    }

    @Override
    public String getName() {
        return this.name;
    }
  
    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDisplayName(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public boolean hasJoinedBefore() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void kick() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void kick(String reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ban() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ban(boolean kick) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ban(boolean kick, String reason) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setVisible(Entity entity, boolean visible) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Entity> getInvisibleEntities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isInvisible(Entity entity) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PlayerSnapshot snapshot() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasPermission(String permission) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasPermission(String permission, PermissionDomain domain) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isInGroup(String group) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isInGroup(String group, PermissionDomain domain) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getGroups() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getGroups(PermissionDomain domain) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PlayerNetwork getNetwork() {
        return network;
    }

    @Override
    public void processCommand(String commandLine) throws CommandException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendCommand(String command, String... args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendMessage(ChatReceiver from, String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendMessage(String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendMessageRaw(String message, String type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TransformProvider getTransformProvider() {
        return transformProvider;
    }

    @Override
    public void setTransformProvider(TransformProvider provider) {
        this.transformProvider = provider == null ? TransformProvider.NullTransformProvider.INSTANCE : provider;
    }

    public void copyInput(FlowWorld world) {
        synchronized (inputMutex) {
            LinkedList<InputSnapshot> snapshot = new LinkedList<InputSnapshot>();
            inputSnapshots.put(world.getThread().getThread().getThreadGroup(), snapshot);
            ConcurrentLinkedQueue<InputSnapshot> get = liveInput.getIfPresent(world);
            if (get == null) {
                return;
            }
            while (!get.isEmpty()) {
                snapshot.add(get.poll());
            }
        }
    }

    /**
     * @return the input for the current thread
     */
    // TODO: I want to store this by thread id, but this is called by asyncmanagers
    @Override
    public List<InputSnapshot> getInput() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        List<InputSnapshot> get = inputSnapshots.get(group);
        if (get == null) {
            get = Collections.EMPTY_LIST;
            inputSnapshots.put(group, get);
        }
        return get;
    }

    public void addInputChanges(float dt, boolean mouseGrabbed, List<KeyboardEvent> keyEvents, List<MouseEvent> mouseEvents) {
        synchronized (inputMutex) {
            for (World world : network.getSession().getEngine().getWorldManager().getWorlds()) {
                ConcurrentLinkedQueue<InputSnapshot> get = liveInput.getIfPresent(world);
                if (get == null) {
                    get = new ConcurrentLinkedQueue<>();
                    liveInput.put((FlowWorld) world, get);
                }
                get.add((lastLiveInput = lastLiveInput.withChanges(dt, mouseGrabbed, keyEvents, mouseEvents)));
            }
        }
    }
}
