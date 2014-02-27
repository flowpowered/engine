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
package com.flowpowered.engine.network.codec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.flowpowered.api.input.KeyboardEvent;
import com.flowpowered.api.input.MouseEvent;
import com.flowpowered.engine.network.message.InputSnapshotMessage;
import com.flowpowered.networking.Codec;
import io.netty.buffer.ByteBuf;
import org.lwjgl.input.Keyboard;

public class InputSnapshotCodec implements Codec<InputSnapshotMessage> { 
    static {
        // This provide some warning if there comes a point where we can't use a byte
        if (Keyboard.getKeyCount() > 255) {
            throw new IllegalStateException("Keyboard size is not less than 256. InputSnapshotCodec must be changed from a byte!");
        }
    }

    @Override
    public InputSnapshotMessage decode(ByteBuf buf) throws IOException {
        List<KeyboardEvent> keyEvents = new LinkedList<>();
        int amount = buf.readInt();
        for (int i = 0; i < amount; i++) {
            keyEvents.add(new KeyboardEvent(buf.readByte(), buf.readBoolean()));
        }
        amount = buf.readInt();
        List<MouseEvent> mouseEvents = new LinkedList<>();
        for (int i = 0; i < amount; i++) {
            mouseEvents.add(new MouseEvent(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean()));
        }     

        return new InputSnapshotMessage(buf.readFloat(), buf.readBoolean(), keyEvents, mouseEvents);
    }

    @Override
    public ByteBuf encode(ByteBuf buf, InputSnapshotMessage message) throws IOException {
        buf.writeInt(message.getKeyEvents().size());
        for (KeyboardEvent e : message.getKeyEvents()) {
            buf.writeByte(e.getKeyId());
            buf.writeBoolean(e.wasPressedDown());
        }
        buf.writeInt(message.getMouseEvents().size());
        for (MouseEvent e : message.getMouseEvents()) {
            buf.writeInt(e.getX());
            buf.writeInt(e.getY());
            buf.writeInt(e.getDX());
            buf.writeInt(e.getDY());
            buf.writeInt(e.getDWheel());
            buf.writeInt(e.getButton());
            buf.writeBoolean(e.wasPressedDown());
        }
        buf.writeFloat(message.getDt());
        buf.writeBoolean(message.isMouseGrabbed());
        return buf;
    }

}
