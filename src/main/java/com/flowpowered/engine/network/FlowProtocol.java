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

import com.flowpowered.engine.network.codec.ChunkDataCodec;
import com.flowpowered.engine.network.codec.InputSnapshotCodec;
import com.flowpowered.engine.network.codec.LoginCodec;
import com.flowpowered.engine.network.codec.UpdateEntityCodec;
import com.flowpowered.engine.network.handler.ChunkDataHandler;
import com.flowpowered.engine.network.handler.InputSnapshotHandler;
import com.flowpowered.engine.network.handler.LoginHandler;
import com.flowpowered.engine.network.handler.UpdateEntityHandler;
import com.flowpowered.engine.network.message.ChunkDataMessage;
import com.flowpowered.engine.network.message.InputSnapshotMessage;
import com.flowpowered.engine.network.message.LoginMessage;
import com.flowpowered.engine.network.message.UpdateEntityMessage;
import com.flowpowered.networking.Codec;
import com.flowpowered.networking.exception.IllegalOpcodeException;
import com.flowpowered.networking.exception.UnknownPacketException;
import com.flowpowered.networking.protocol.simple.SimpleProtocol;

import io.netty.buffer.ByteBuf;

public class FlowProtocol extends SimpleProtocol {
    public static final FlowProtocol INSTANCE = new FlowProtocol();

    private FlowProtocol() {
        super("FlowProtocol", 4);

        registerMessage(LoginMessage.class, LoginCodec.class, LoginHandler.class, 0);
        registerMessage(ChunkDataMessage.class, ChunkDataCodec.class, ChunkDataHandler.class, 1);
        registerMessage(InputSnapshotMessage.class, InputSnapshotCodec.class, InputSnapshotHandler.class, 2);
        registerMessage(UpdateEntityMessage.class, UpdateEntityCodec.class, UpdateEntityHandler.class, 3);
        
    }

    @Override
    public Codec<?> readHeader(ByteBuf buf) throws UnknownPacketException {
        byte opcode = -1;
        try {
            opcode = buf.readByte();
            return getCodecLookupService().find(opcode);
        } catch (IllegalOpcodeException ex) {
            throw new UnknownPacketException("Unknown packet", opcode, -1);
        }
    }

    @Override
    public ByteBuf writeHeader(ByteBuf header, Codec.CodecRegistration codec, ByteBuf data) {
        return header.writeByte(codec.getOpcode());
    }

}
