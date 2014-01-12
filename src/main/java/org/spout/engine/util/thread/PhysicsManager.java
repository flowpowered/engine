package org.spout.engine.util.thread;

public interface PhysicsManager extends AsyncManager {
    /**
     * This method is called to execute physics for blocks local to the Region. It might be called multiple times per tick
     *
     * @param sequence -1 for local, 0 - 26 for which sequence
     */
    void runPhysics(int sequence);
}