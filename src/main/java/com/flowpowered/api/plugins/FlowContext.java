package com.flowpowered.api.plugins;

import com.flowpowered.api.Engine;
import com.flowpowered.plugins.Context;
import com.flowpowered.plugins.Plugin;

public class FlowContext extends Context {
    private final Engine engine;

    public FlowContext(Plugin<?> plugin, Engine engine) {
        super(plugin);
        this.engine = engine;
    }

    public Engine getEngine() {
        return engine;
    }
}
