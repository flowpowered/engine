package org.spout.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.flowpowered.commons.StringUtil;

import org.spout.api.Platform;
import org.spout.api.Server;
import org.spout.api.entity.Player;
import org.spout.api.geo.ServerWorldManager;
import org.spout.engine.entity.SpoutPlayer;
import org.spout.engine.geo.SpoutServerWorldManager;
import org.spout.engine.util.thread.snapshotable.SnapshotableLinkedHashMap;

public class SpoutServer extends SpoutEngine implements Server {
    protected final SnapshotableLinkedHashMap<String, SpoutPlayer> players;
    private final SpoutServerWorldManager worldManager;

    public SpoutServer(SpoutApplication args) {
        super(args);
        players = new SnapshotableLinkedHashMap<>(snapshotManager);
        worldManager = new SpoutServerWorldManager(this);
    }

    @Override
    public Platform getPlatform() {
        return Platform.SERVER;
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
		Map<String, SpoutPlayer> playerList = players.get();
		ArrayList<Player> onlinePlayers = new ArrayList<>(playerList.size());
		for (SpoutPlayer player : playerList.values()) {
			if (player.isOnline()) {
				onlinePlayers.add(player);
			}
		}
		return onlinePlayers;
    }

    @Override
    public int getMaxPlayers() {
        // TODO: config
        return 5;
    }

    @Override
    public void broadcastMessage(String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void broadcastMessage(String permission, String message) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Player getPlayer(String name, boolean exact) {
		name = name.toLowerCase();
		if (exact) {
			for (Player player : players.getValues()) {
				if (player.getName().equalsIgnoreCase(name)) {
					return player;
				}
			}
			return null;
		} else {
			return StringUtil.getShortest(StringUtil.matchName(players.getValues(), name));
		}
    }

    @Override
    public SpoutServerWorldManager getWorldManager() {
        return worldManager;
    }
}
