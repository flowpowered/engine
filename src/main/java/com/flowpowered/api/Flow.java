/*
 * This file is part of Flow Engine, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.flowpowered.events.EventManager;
import com.flowpowered.filesystem.FileSystem;

/**
 * Represents the Flow core, to get singleton {@link Engine} instance
 */
public final class Flow {
    private static Engine instance = null;
    private static final Logger logger = LogManager.getLogger("Flow");

    private Flow() {
        throw new IllegalStateException("Can not construct Flow instance");
    }

    /**
     * Gets the {@link Logger} instance that is used to write to the console.
     *
     * @return logger
     */
    public static Logger getLogger() {
        return logger;
    }

    /**
     * Prints the specified object if debug mode is enabled.
     *
     * @param obj to print
     */
    public static void debug(Object obj) {
        if (debugMode()) {
            info(obj.toString());
        }
    }

    /**
     * Logs the specified message to print if debug mode is enabled.
     *
     * @param log message
     * @param t to throw
     * @see #debugMode()
     */
    public static void debug(String log, Throwable t) {
        if (debugMode()) {
            info(log, t);
        }
    }

    /**
     * Logs the specified message to print if debug mode is enabled.
     *
     * @param log message
     * @param params of message
     * @see #debugMode()
     */
    public static void debug(String log, Object... params) {
        if (debugMode()) {
            info(log, params);
        }
    }

    public static void finest(String log, Throwable t) {
        logger.log(Level.TRACE, log, t);
    }

    public static void finest(String log, Object... params) {
        logger.log(Level.TRACE, log, params);
    }

    public static void finer(String log, Throwable t) {
        logger.log(Level.TRACE, log, t);
    }

    public static void finer(String log, Object... params) {
        logger.log(Level.TRACE, log, params);
    }

    public static void fine(String log, Throwable t) {
        logger.log(Level.DEBUG, log, t);
    }

    public static void fine(String log, Object... params) {
        logger.log(Level.DEBUG, log, params);
    }

    public static void info(String log, Throwable t) {
        logger.log(Level.INFO, log, t);
    }

    public static void info(String log, Object... params) {
        logger.log(Level.INFO, log, params);
    }

    public static void warn(String log, Throwable t) {
        logger.log(Level.WARN, log, t);
    }

    public static void warn(String log, Object... params) {
        logger.log(Level.WARN, log, params);
    }

    public static void severe(String log, Throwable t) {
        logger.log(Level.ERROR, log, t);
    }

    public static void severe(String log, Object... params) {
        logger.log(Level.ERROR, log, params);
    }

    public static void setEngine(Engine game) {
        if (instance == null) {
            instance = game;
        } else {
            throw new UnsupportedOperationException("Can not redefine singleton Game instance");
        }
    }

    /**
     * Gets the currently running engine instance.
     *
     * @return engine
     */
    public static Engine getEngine() {
        return instance;
    }

    /**
     * Ends this game instance safely. All worlds, players, and configuration data is saved, and all threads are ended cleanly.<br/> <br/> Players will be sent a default disconnect message.
     */
    public static void stop() {
        instance.stop();
    }

    /**
     * Returns the game's {@link EventManager} Event listener registration and calling is handled through this.
     *
     * @return Our EventManager instance
     */
    public static EventManager getEventManager() {
        return instance.getEventManager();
    }
    /**
     * Returns the {@link Platform} that the game is currently running on.
     *
     * @return current platform type
     */
    public static Platform getPlatform() {
        return instance.getPlatform();
    }

    /**
     * Returns true if the game is running in debug mode <br/> <br/> To start debug mode, start Flow with -debug
     *
     * @return true if server is started with the -debug flag, false if not
     */
    public static boolean debugMode() {
        return instance.debugMode();
    }

    /**
     * Logs the given string using {@Link Logger#info(String)} to the default logger instance.
     *
     * @param arg to log
     */
    public static void log(String arg) {
        logger.info(arg);
    }

    /**
     * Returns the String version of the API.
     *
     * @return version
     */
    public static String getAPIVersion() {
        return instance.getClass().getPackage().getImplementationVersion();
    }

    /**
     * Gets an abstract representation of the engine's {@link FileSystem}.<br/> <br/> The Filesystem handles the loading of all resources.<br/> <br/> On the client, loading a resource will load the
     * resource from the harddrive.<br/> On the server, it will notify all clients to load the resource, as well as provide a representation of that resource.
     *
     * @return filesystem from the engine.
     */
    public static FileSystem getFileSystem() {
        return instance.getFileSystem();
    }
}
