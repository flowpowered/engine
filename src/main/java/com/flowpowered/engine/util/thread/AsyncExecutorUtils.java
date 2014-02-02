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
package com.flowpowered.engine.util.thread;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import com.flowpowered.api.Flow;

public class AsyncExecutorUtils {
    private static final String LINE = "------------------------------";

    /**
     * Logs all threads, the thread details, and active stack traces
     */
    public static void dumpAllStacks() {
        Logger log = Flow.getLogger();
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
        Iterator<Entry<Thread, StackTraceElement[]>> i = traces.entrySet().iterator();
        while (i.hasNext()) {
            Entry<Thread, StackTraceElement[]> entry = i.next();
            Thread thread = entry.getKey();
            log.info(LINE);

            log.info("Current Thread: " + thread.getName());
            log.info("    PID: " + thread.getId() + " | Alive: " + thread.isAlive() + " | State: " + thread.getState());
            log.info("    Stack:");
            StackTraceElement[] stack = entry.getValue();
            for (int line = 0; line < stack.length; line++) {
                log.info("        " + stack[line].toString());
            }
        }
        log.info(LINE);
    }

    /**
     * Scans for deadlocked threads
     */
    public static void checkForDeadlocks() {
        Logger log = Flow.getLogger();
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        long[] ids = tmx.findDeadlockedThreads();
        if (ids != null) {
            log.info("Checking for deadlocks");
            ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
            log.info("The following threads are deadlocked:");
            for (ThreadInfo ti : infos) {
                log.info(ti.toString());
            }
        }
    }

    /**
     * Dumps the stack for the given Thread
     *
     * @param t the thread
     */
    public static void dumpStackTrace(Thread t) {
        StackTraceElement[] stackTrace = t.getStackTrace();
        Flow.getLogger().info("Stack trace for Thread " + t.getName());
        for (StackTraceElement e : stackTrace) {
            Flow.getLogger().info("\tat " + e);
        }
    }
}
