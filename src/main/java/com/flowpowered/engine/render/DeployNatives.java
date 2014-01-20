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
package com.flowpowered.engine.render;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

public class DeployNatives {
    public static void deploy() throws Exception {
        final String osPath;
        final String[] nativeLibs;
        if (SystemUtils.IS_OS_WINDOWS) {
            nativeLibs = new String[]{
                    "jinput-dx8_64.dll", "jinput-dx8.dll", "jinput-raw_64.dll", "jinput-raw.dll",
                    "jinput-wintab.dll", "lwjgl.dll", "lwjgl64.dll", "OpenAL32.dll", "OpenAL64.dll"
            };
            osPath = "windows/";
        } else if (SystemUtils.IS_OS_MAC) {
            nativeLibs = new String[]{
                    "libjinput-osx.jnilib", "liblwjgl.jnilib", "openal.dylib"
            };
            osPath = "mac/";
        } else if (SystemUtils.IS_OS_LINUX) {
            nativeLibs = new String[]{
                    "liblwjgl.so", "liblwjgl64.so", "libopenal.so", "libopenal64.so", "libjinput-linux.so",
                    "libjinput-linux64.so"
            };
            osPath = "linux/";
        } else {
            throw new IllegalStateException("Could not get lwjgl natives for OS \"" + SystemUtils.OS_NAME + "\".");
        }
        final File nativesDir = new File("natives" + File.separator + osPath);
        nativesDir.mkdirs();
        for (String nativeLib : nativeLibs) {
            final File nativeFile = new File(nativesDir, nativeLib);
            if (!nativeFile.exists()) {
                FileUtils.copyInputStreamToFile(DeployNatives.class.getResourceAsStream("/" + nativeLib), nativeFile);
            }
        }
        final String nativesPath = nativesDir.getAbsolutePath();
        System.setProperty("org.lwjgl.librarypath", nativesPath);
        System.setProperty("net.java.games.input.librarypath", nativesPath);
    }
}
