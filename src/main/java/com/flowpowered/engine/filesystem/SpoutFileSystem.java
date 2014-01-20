package com.flowpowered.engine.filesystem;

import java.io.File;
import com.flowpowered.filesystem.SimpleFileSystem;

public class SpoutFileSystem extends SimpleFileSystem {
	public static final File PLAYERS_DIRECTORY = new File("players");
	public static final File WORLDS_DIRECTORY = new File("worlds");
}
