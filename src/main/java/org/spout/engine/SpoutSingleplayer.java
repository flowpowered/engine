package org.spout.engine;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spout.api.Platform;
import org.spout.api.Singleplayer;
import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.generator.SolidWorldGenerator;
import org.spout.api.geo.LoadOption;
import org.spout.api.geo.World;
import org.spout.api.material.BlockMaterial;
import org.spout.engine.entity.SpoutPlayer;
import org.spout.engine.geo.world.SpoutWorld;
import org.spout.engine.render.DeployNatives;
import org.spout.engine.render.SpoutRenderer;
import com.flowpowered.math.vector.Vector3f;

public class SpoutSingleplayer extends SpoutServer implements Singleplayer {
    private final AtomicReference<SpoutPlayer> player = new AtomicReference<>();
    private final AtomicReference<SpoutWorld> activeWorld = new AtomicReference<>();

    // TEST CODE
    private Entity testEntity;

    public SpoutSingleplayer(SpoutApplication args) {
        super(args);
    }

    @Override
    public void init() {
        try {
            DeployNatives.deploy();
        } catch (Exception ex) {
            Logger.getLogger(SpoutSingleplayer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        super.init();
        SpoutWorld loadedWorld = getWorldManager().loadWorld("fallback", new SolidWorldGenerator(BlockMaterial.SOLID_BLUE));
        activeWorld.set(loadedWorld);
        SpoutPlayer player = new SpoutPlayer("Spout");
        this.player.set(player);
        players.put(player.getName(), player);
        Entity entity = loadedWorld.spawnEntity(Vector3f.ZERO, LoadOption.LOAD_GEN);
        this.testEntity = entity;
    }

    public Entity getTestEntity() {
        return testEntity;
    }

    @Override
    public void start() {
        getScheduler().startClientThreads();
        super.start();
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
