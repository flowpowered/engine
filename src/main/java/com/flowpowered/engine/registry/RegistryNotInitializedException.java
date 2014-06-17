package com.flowpowered.engine.registry;

import com.flowpowered.api.Flow;

/**
 * RunTimeException which is thrown when the Registry is not initialized yet.
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class RegistryNotInitializedException extends RuntimeException {

	private Exception exception;

	public RegistryNotInitializedException(final String registryName) {
		super("The " + registryName + " has not been initialized!");
		exception = null;
		Flow.severe("The " + registryName + " is not initialized yet!");
		Flow.severe("This is a major error and you should notify the developer!");
	}

	public RegistryNotInitializedException() {
		super("The registry has not been initialized!");
		exception = null;
		Flow.severe("This registry is not initialized yet!");
		Flow.severe("This is a major error and you should notify the developer!");
	}

	public RegistryNotInitializedException(final String registryName, Exception ex) {
		super("The " + registryName + " has not been initialized!", ex);
		exception = ex;
		Flow.severe("The " + registryName + " is not initialized yet!");
		Flow.severe("The following Exception was thrown:", exception);
		Flow.getLogger().severe("This is a major error and you should notify the developer!");
	}

	public String getMessage() {
		return "The registry has not been initialized!, please contact the developer as this is a major error!";
	}

	public Exception getException() {
		return exception;
	}
}
