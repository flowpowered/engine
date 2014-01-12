package org.spout.engine.util.thread;

public interface DynamicUpdateManager extends AsyncManager {
    /**
     * This method is called to execute dynamic updates for blocks in the Region. It might be called multiple times per tick, the sequence number indicates which lists to check
     *
     * @param sequence -1 for local, 0 - 26 for which sequence
     */
    void runDynamicUpdates(long threshold, int sequence);

	/**
	 * This method is called to determine the earliest available dynamic update time
	 *
	 * @return the earliest pending dynamic block update
	 */
	public long getFirstDynamicUpdateTime();
}