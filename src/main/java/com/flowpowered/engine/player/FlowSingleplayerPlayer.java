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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.flowpowered.api.geo.discrete.TransformProvider;
import com.flowpowered.api.input.InputSnapshot;
import com.flowpowered.api.player.ClientPlayer;
import com.flowpowered.api.player.Player;
import com.flowpowered.api.player.PlayerNetwork;
import com.flowpowered.chat.ChatReceiver;
import com.flowpowered.commands.CommandException;
import com.flowpowered.engine.network.FlowSession;
import com.flowpowered.permissions.PermissionDomain;

public class FlowSingleplayerPlayer implements ClientPlayer {
    private final Player player;
    private final PlayerNetwork network;

    public FlowSingleplayerPlayer(FlowSession session, Player player) {
        this.player = player;
        this.network = new PlayerNetwork(session, this);
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public PlayerNetwork getNetwork() {
        return network; 
    }

    @Override
    public void sendCommand(String command, String... args) {
        player.sendCommand(command, args);
    }

    @Override
    public TransformProvider getTransformProvider() {
        return player.getTransformProvider();
    }

    @Override
    public void setTransformProvider(TransformProvider provider) {
        player.setTransformProvider(provider);
    }

    @Override
    public void processCommand(String commandLine) throws CommandException {
        player.processCommand(commandLine);
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public void sendMessage(ChatReceiver from, String message) {
        player.sendMessage(from, message);
    }

    @Override
    public void sendMessageRaw(String message, String type) {
        player.sendMessageRaw(message, type);
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission, PermissionDomain domain) {
        return player.hasPermission(permission, domain);
    }

    @Override
    public boolean isInGroup(String group) {
        return player.isInGroup(group);
    }

    @Override
    public boolean isInGroup(String group, PermissionDomain domain) {
        return player.isInGroup(group, domain);
    }

    @Override
    public Set<String> getGroups() {
        return player.getGroups();
    }

    @Override
    public Set<String> getGroups(PermissionDomain domain) {
        return player.getGroups(domain);
    }

    @Override
    public List<InputSnapshot> getInput() {
        return player.getInput();
    }

    @Override
    public void setInput(List<InputSnapshot> inputList) {
        player.setInput(inputList);
    }
}
