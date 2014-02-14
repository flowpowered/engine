package com.flowpowered.engine.network.handler;

import com.flowpowered.api.Flow;
import com.flowpowered.engine.network.FlowSession;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.MessageHandler;

public class FlowMessageHandler<T extends Message> implements MessageHandler<FlowSession, T> {

    @Override
    public void handle(FlowSession session, T message) {
        if (Flow.getEngine().getPlatform().isClient()) {
            handleClient(session, message);
        } else {
            handleServer(session, message);
        }
    }

    public void handleServer(FlowSession session, T message) {}

    public void handleClient(FlowSession session, T message) {}
}
