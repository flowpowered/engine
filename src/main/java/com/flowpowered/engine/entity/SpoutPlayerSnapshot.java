package com.flowpowered.engine.entity;

import java.lang.ref.WeakReference;

import com.flowpowered.api.entity.Player;
import com.flowpowered.api.entity.PlayerSnapshot;
import com.flowpowered.api.geo.discrete.Transform;

public class SpoutPlayerSnapshot implements PlayerSnapshot {
    private final Transform cameraLocation;
    private final String name;
    private final WeakReference<Player> player;
    private final long time;

    public SpoutPlayerSnapshot(Player player) {
        this.cameraLocation = player.getCameraLocation().copy();
        this.name = player.getName();
        this.player = new WeakReference<>(player);
        this.time = System.currentTimeMillis();
    }

    @Override
    public Transform getCameraLocation() {
        return cameraLocation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Player getReference() {
        return player.get();
    }

    @Override
    public long getSnapshotTime() {
        return time;
    }
}
