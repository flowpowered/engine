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
package com.flowpowered.engine.network.handler;

import com.flowpowered.engine.network.FlowSession;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.MessageHandler;

public class FlowMessageHandler<T extends Message> implements MessageHandler<FlowSession, T> {

    @Override
    public void handle(FlowSession session, T message) {
        if (session.getEngine().getPlatform().isServer()) {
            handle0(session, message, true);
        } else {
            handle0(session, message, false);
        }
    }

    public void handle0(FlowSession session, T message, boolean server) {
        if (session.getPlayer() == null) {
            switch (requiresPlayer()) {
                case ERROR:
                    throw new UnsupportedOperationException("Handler " + getClass() + " requires a player, but it was null!");
                case IGNORE:
                    System.out.println("Ignoring handler b/c no player");
                    return;
            }
        }
        if (server) {
            handleServer(session, message);
        } else {
            handleClient(session, message);
        }
    }

    public void handleServer(FlowSession session, T message) {
        throw new UnsupportedOperationException("This handler cannot handle on the server!");
    }

    public void handleClient(FlowSession session, T message) {
        throw new UnsupportedOperationException("This handler cannot handle on the client!");
    }

    public PlayerRequirement requiresPlayer() {
        return PlayerRequirement.NO;
    }

    public enum PlayerRequirement {
        NO,
        IGNORE,
        ERROR;
    }
}
