package com.flowpowered.engine.registry;

import com.flowpowered.api.Flow;

/**
 * RunTimeException which is thrown when the Registry can not be loaded or saved.
 *
 * @author $Author: dredhorse$
 * @version $FullVersion$*
 */
public class RegistryIOException extends RuntimeException {

	private Exception exception;

	public RegistryIOException(final String registryName, final String ioKind) {
		super("The " + registryName + " can not be " + ioKind + "!");
		exception = null;
		Flow.severe("The " + registryName + " can not be " + ioKind + "!");
		Flow.severe("This is a major error and you should check your filesystem!");
		Flow.severe("NOTE: Without fixing this worlds can not be " + ioKind + " correctly !");
	}

	public RegistryIOException() {
		super("The registry has some IO errors!");
		exception = null;
		Flow.severe("This is a major error and you should check your filesystem!");
		Flow.severe("NOTE: Without fixing this worlds can not be correctly handled!");
	}

	public RegistryIOException(final String registryName, final String ioKind, Exception ex) {
		super("The " + registryName + " can not be " + ioKind + "!", ex);
		exception = ex;
		Flow.severe("The " + registryName + " can not be " + ioKind + "!");
		Flow.severe("This is a major error and you should check your filesystem!");
		Flow.severe("NOTE: Without fixing this worlds can not be " + ioKind + " correctly !");
	}

	public String getMessage() {
		return "The registry has some IO errors!, please check your file system as this is a major error!";
	}

	public Exception getException() {
		return exception;
	}
}
