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
