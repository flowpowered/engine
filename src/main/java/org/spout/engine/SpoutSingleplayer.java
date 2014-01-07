package org.spout.engine;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spout.api.Platform;
import org.spout.api.Singleplayer;
import org.spout.api.entity.Player;
import org.spout.api.generator.FlatWorldGenerator;
import org.spout.api.geo.World;
import org.spout.api.material.BlockMaterial;
import org.spout.engine.entity.SpoutPlayer;
import org.spout.engine.geo.SpoutWorld;
import org.spout.engine.render.DeployNatives;
import org.spout.engine.render.SpoutRenderer;

public class SpoutSingleplayer extends SpoutServer implements Singleplayer {
    private final AtomicReference<SpoutPlayer> player = new AtomicReference<>();
    private final AtomicReference<SpoutWorld> activeWorld = new AtomicReference<>();

    public SpoutSingleplayer(SpoutApplication args) {
        super(args);
    }


    @Override
    public void start() {
        super.start();
        try {
            DeployNatives.deploy();
        } catch (Exception ex) {
            Logger.getLogger(SpoutSingleplayer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        getScheduler().startRenderThread();
        getWorldManager().loadWorld("fallback", new FlatWorldGenerator(BlockMaterial.SOLID_BLUE));
    }

    @Override
    public boolean stop() {
        return super.stop();
        
    }

    @Override
    public Platform getPlatform() {
        return Platform.SINGLEPLAYER;
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
    public SpoutRenderer getRenderer() {
        return getScheduler().getRenderThread().getRenderer();
    }

}
