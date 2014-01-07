package org.spout.engine.scheduler;

import org.lwjgl.opengl.Display;
import org.spout.engine.render.SpoutRenderer;
import org.spout.renderer.GLVersioned;

public class RenderThread extends SchedulerElement {
    private final SpoutScheduler scheduler;
    private final SpoutRenderer renderer;

    public RenderThread(SpoutScheduler scheduler) {
        super("RenderThread", 60);
        this.scheduler = scheduler;
        this.renderer = new SpoutRenderer();
    }

    @Override
    public void onStart() {
        renderer.setGLVersion(GLVersioned.GLVersion.GL30);
        renderer.init();
    }

    @Override
    public void onStop() {
        renderer.dispose();
    }

    @Override
    public void onTick(long dt) {
        if (Display.isCloseRequested()) {
            scheduler.stop();
        }
        renderer.render();
    }

    public SpoutRenderer getRenderer() {
        return renderer;
    }
}
