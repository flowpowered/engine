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

import com.flowpowered.api.geo.discrete.Transform;
import com.flowpowered.api.player.reposition.NullRepositionManager;
import com.flowpowered.engine.network.message.UpdateEntityMessage;
import com.flowpowered.engine.network.message.UpdateEntityMessage.UpdateAction;
import com.flowpowered.engine.util.FlowByteBufUtils;
import com.flowpowered.networking.Codec;
import io.netty.buffer.ByteBuf;

public class UpdateEntityCodec implements Codec<UpdateEntityMessage> {

	@Override
	public ByteBuf encode(ByteBuf buf, UpdateEntityMessage message) {
        buf.writeByte(message.getAction().ordinal());
        buf.writeInt(message.getEntityId());
		switch (message.getAction()) {
			case REMOVE:
				break;
			case ADD:
			case TRANSFORM:
				FlowByteBufUtils.writeTransform(buf, message.getTransform());
				break;
			case POSITION:
				throw new UnsupportedOperationException("Position is unimplemented!");
			default:
				throw new IllegalArgumentException("Unknown UpdateAction!");
		}
		return buf;
	}

	@Override
	public UpdateEntityMessage decode(ByteBuf buffer) {
		final byte actionByte = buffer.readByte();
		if (actionByte < 0 || actionByte >= UpdateAction.values().length) {
			throw new IllegalArgumentException("Unknown response ID " + actionByte);
		}

		final UpdateAction action = UpdateAction.values()[actionByte];
		final int entityId = buffer.readInt();
		final Transform transform;
		switch (action) {
			case REMOVE:
				transform = null;
				break;
			case ADD:
			case TRANSFORM:
				transform = FlowByteBufUtils.readTransform(buffer);
				break;
			case POSITION:
				throw new UnsupportedOperationException("Position is unimplemented!");
			default:
				throw new IllegalArgumentException("Unknown UpdateAction!");
		}

		return new UpdateEntityMessage(entityId, transform, action, NullRepositionManager.INSTANCE);
	}
}
