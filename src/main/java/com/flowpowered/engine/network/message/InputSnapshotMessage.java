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
package com.flowpowered.engine.network.message;

import java.util.List;

import com.flowpowered.api.input.KeyboardEvent;
import com.flowpowered.api.input.MouseEvent;
import com.flowpowered.networking.Message;

public class InputSnapshotMessage implements Message {
    private final float dt;
    private final boolean mouseGrabbed;
    private final List<KeyboardEvent> keyEvents;
    private final List<MouseEvent> mouseEvents;

    public InputSnapshotMessage(float dt, boolean mouseGrabbed, List<KeyboardEvent> keyEvents, List<MouseEvent> mouseEvents) {
        this.dt = dt;
        this.mouseGrabbed = mouseGrabbed;
        this.keyEvents = keyEvents;
        this.mouseEvents = mouseEvents;
    }

    public float getDt() {
        return dt;
    }

    public boolean isMouseGrabbed() {
        return mouseGrabbed;
    }

    public List<KeyboardEvent> getKeyEvents() {
        return keyEvents;
    }

    public List<MouseEvent> getMouseEvents() {
        return mouseEvents;
    }

}
