package com.flowpowered.engine;

import java.util.concurrent.atomic.AtomicReference;
import com.flowpowered.api.Client;
import com.flowpowered.api.Platform;
import com.flowpowered.api.entity.Player;
import com.flowpowered.api.geo.World;
import com.flowpowered.api.geo.WorldManager;
import com.flowpowered.api.render.Renderer;
import com.flowpowered.engine.entity.SpoutPlayer;
import com.flowpowered.engine.geo.world.SpoutWorld;
import com.flowpowered.engine.geo.world.SpoutWorldManager;

public class SpoutClient extends SpoutEngine implements Client {
    private final AtomicReference<SpoutPlayer> player = new AtomicReference<>();
    private final AtomicReference<SpoutWorld> activeWorld = new AtomicReference<>();
    private final SpoutWorldManager worldManager;

    public SpoutClient(SpoutApplication args) {
        super(args);
        this.worldManager = new SpoutWorldManager(this);
    }

    @Override
    public Platform getPlatform() {
        return Platform.CLIENT;
    }

    @Override
    public WorldManager getWorldManager() {
        return worldManager;
    }

    @Override
    public Player getPlayer() {
        return player.get();
    }

    @Override
    public World getWorld() {
        return activeWorld.get();
    }

    @Override
    public Renderer getRenderer() {
        return getScheduler().getRenderThread().getRenderer();
    }

}
