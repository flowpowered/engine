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
package com.flowpowered.engine;

import java.net.InetSocketAddress;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import com.flowpowered.api.Platform;

import com.flowpowered.engine.util.argument.PlatformConverter;

/**
 * A main class for launching various platforms
 */
public class FlowApplication {
    @Parameter(names = {"--platform", "-platform", "--p", "-p"}, converter = PlatformConverter.class)
    public Platform platform = Platform.SINGLEPLAYER;
    @Parameter(names = {"--debug", "-debug", "--d", "-d"}, description = "Debug Mode")
    public boolean debug = false;
    @Parameter(names = {"--protocol"}, description = "Protocol to connect with")
    public String protocol = null;
    @Parameter(names = {"--server"}, description = "Server to connect to")
    public String server = "localhost";
    @Parameter(names = {"--port"}, description = "Port to connect to")
    public int port = 25565;
    @Parameter(names = {"--user"}, description = "User to connect as")
    public String user = null;

    public static void main(String[] args) {
        try {
            FlowApplication main = new FlowApplication();
            JCommander commands = new JCommander(main);
            commands.parse(args);

            FlowEngineImpl engine = new FlowEngineImpl();
            engine.init(main.debug);
            engine.start();

            switch (main.platform) {
                case CLIENT:
                    FlowClientImpl client = new FlowClientImpl(engine);
                    client.setServerAddress(new InetSocketAddress(main.server, main.port));
                    engine.add(client);
                    break;
                case SERVER:
                    engine.add(main.makeServer(engine));
                    break;
                case SINGLEPLAYER:
                    engine.add(main.makeServer(engine));
                    engine.add(new FlowSingleplayerImpl(engine));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown platform: " + main.platform);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Runtime.getRuntime().halt(1);
        }
    }

    protected FlowServerImpl makeServer(FlowEngineImpl engine) {
        FlowServerImpl server = new FlowServerImpl(engine);
        server.setBindAddress(new InetSocketAddress(port));
        return server;
    }
}
