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

import com.flowpowered.events.EventManager;
import com.flowpowered.filesystem.FileSystem;

/**
 * Represents the Flow core, to get singleton {@link Engine} instance
 */
public final class Flow {
    private static Engine instance = null;

    private Flow() {
        throw new IllegalStateException("Can not construct Flow instance");
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
