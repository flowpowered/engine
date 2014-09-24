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
package com.flowpowered.engine.physics;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.flowpowered.engine.util.math.ReactConverter;
import org.spout.physics.engine.DynamicsWorld;

public class PhysicsManager {
    private final DynamicsWorld simulation;
    private final LinkedBlockingQueue<Consumer<DynamicsWorld>> preUpdateQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<Consumer<DynamicsWorld>> postUpdateQueue = new LinkedBlockingQueue<>();

    public PhysicsManager() {
        simulation = new DynamicsWorld(ReactConverter.toReactVector3(0f, -9.81f, -0f), 1/20f);
        simulation.start();
    }

    public void start() {
        simulation.start();
    }

    public void stop() {
        simulation.stop();
    }

    public void update() {
        LinkedList<Consumer<DynamicsWorld>> pre = new LinkedList<>();
        preUpdateQueue.drainTo(pre);
        pre.stream().forEach((c) -> c.accept(simulation));
        simulation.update();
        LinkedList<Consumer<DynamicsWorld>> post = new LinkedList<>();
        postUpdateQueue.drainTo(post);
        post.stream().forEach((c) -> c.accept(simulation));
    }

    public void queuePreUpdateTask(Consumer<DynamicsWorld> task) {
        preUpdateQueue.add(task);
    }

    public void queuePostUpdateTask(Consumer<DynamicsWorld> task) {
        postUpdateQueue.add(task);
    }

    public DynamicsWorld getSimulation() {
        return simulation;
    }
}
