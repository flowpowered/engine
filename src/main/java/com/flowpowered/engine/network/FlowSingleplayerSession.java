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
package com.flowpowered.engine.network;

import java.net.InetSocketAddress;

import com.flowpowered.engine.network.handler.FlowMessageHandler;
import com.flowpowered.networking.Message;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;

public class FlowSingleplayerSession extends FlowSession {
    private final boolean isServer;

    public FlowSingleplayerSession(boolean isServer) {
        super(null);
        this.isServer = isServer;
    }

    @Override
    public void send(Message message) {
        FlowMessageHandler<Message> handler = (FlowMessageHandler<Message>) getProtocol().getMessageHandle(message.getClass());
        if (handler == null) {
            throw new UnsupportedOperationException("Tried to send a Message (" + message.getClass() + ") with no handler!");
        }
        // If we're on the "server", we want to handleClient. And vice-versa
        handler.handle0(this, message, !isServer);
    }

    @Override
    public Channel getChannel() {
        throw new UnsupportedOperationException("FlowSingleplayerSession does not have a channel!");
    }

    @Override
    public InetSocketAddress getAddress() {
        return InetSocketAddress.createUnresolved("127.0.0.1", 25565);
    }

    @Override
    public String toString() {
        return "FlowSingeplayerSession{" + '}';
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void disconnect() {
        // TODO: just shutdown?
        throw new UnsupportedOperationException("Can't disconnect a FlowSingleplayerSession!");
    }

    @Override
    public <T> void setOption(ChannelOption<T> option, T value) {
        throw new UnsupportedOperationException("Can't set an option on a FlowSingleplayerSession!");
    }

}
