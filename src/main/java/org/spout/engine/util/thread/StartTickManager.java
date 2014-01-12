package org.spout.engine.util.thread;

public interface StartTickManager extends AsyncManager {
    /**
     * This method is called in order to start a new tick
     *
     * @param delta the time since the last tick
     */
    void startTickRun(int stage, long delta);
}