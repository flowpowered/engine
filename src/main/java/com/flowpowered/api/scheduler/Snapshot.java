package com.flowpowered.api.scheduler;

public interface Snapshot<T> {
	/**
	 * Returns the reference, if available
	 *
	 * @return reference if it is available, else null
	 */
    T getReference();

	/**
	 * Gets the UTC system clock time at the time this snapshot was created <p> Equivalent to the output of System.currentTimeMillis() </p>
	 *
	 * @return UTC system time
	 */
	public long getSnapshotTime();
}
