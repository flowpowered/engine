package com.flowpowered.engine.network.handler;

import com.flowpowered.api.Flow;
import com.flowpowered.engine.FlowClient;
import com.flowpowered.engine.geo.world.FlowWorld;
import com.flowpowered.engine.network.FlowSession;
import com.flowpowered.engine.network.message.ChunkDataMessage;

public class ChunkDataHandler extends FlowMessageHandler<ChunkDataMessage> {

	@Override
	public void handleClient(FlowSession session, ChunkDataMessage message) {
        FlowClient client = (FlowClient) Flow.getEngine();
        FlowWorld world = client.getWorld();
		if (message.isUnload()) {
            world.setChunk(message.getX(), message.getY(), message.getZ(), null);
		} else {
            world.setChunk(message.getX(), message.getY(), message.getZ(), message.getBlocks());
        }
	}
}
