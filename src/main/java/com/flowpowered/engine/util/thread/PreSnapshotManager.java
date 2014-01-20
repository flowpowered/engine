package com.flowpowered.engine.util.thread;


public interface PreSnapshotManager extends AsyncManager {
    /**
     * This method is called directly before copySnapshotRun and is a MONITOR ONLY stage and no updates should be performed.<br> <br> It occurs after the finalize stage and before the copy snapshot
     * stage.
     */
    void preSnapshotRun();
}
