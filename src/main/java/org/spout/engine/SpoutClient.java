package org.spout.engine;

import java.util.concurrent.atomic.AtomicReference;
import org.spout.api.Client;
import org.spout.api.Platform;
import org.spout.api.entity.Player;
import org.spout.api.geo.World;
import org.spout.api.geo.WorldManager;
import org.spout.api.render.Renderer;
import org.spout.engine.entity.SpoutPlayer;
import org.spout.engine.geo.world.SpoutWorld;
import org.spout.engine.geo.world.SpoutWorldManager;

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
