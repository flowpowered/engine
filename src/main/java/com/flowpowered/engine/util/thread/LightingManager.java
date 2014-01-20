package com.flowpowered.engine.util.thread;

public interface LightingManager extends AsyncManager {
    /**
     * This method is called to update lighting. It might be called multiple times per tick
     *
     * @param sequence -1 for local, 0 - 26 for which sequence
     */
    void runLighting(int sequence);
}