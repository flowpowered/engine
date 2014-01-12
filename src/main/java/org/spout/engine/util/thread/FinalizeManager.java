package org.spout.engine.util.thread;


public interface FinalizeManager extends AsyncManager {
    /**
     * This method is called directly before preSnapshot is called
     */
    void finalizeRun();
}
