package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;

public final class CoreModule implements HydroModule {

    @Override
    public String id() {
        return "core";
    }

    @Override
    public String displayName() {
        return "Core";
    }

    @Override
    public String description() {
        return "Hydroxide administration, reloads, and module status.";
    }

    @Override
    public void onEnable(HydroxideContext context) {
        context.commands().register("hydroxide", new HydroxideCommand(context));
    }
}
