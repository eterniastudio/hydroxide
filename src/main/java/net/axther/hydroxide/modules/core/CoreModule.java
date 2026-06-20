package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

public final class CoreModule implements HydroModule {

    private CommandControlListener commandControlListener;

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
        commandControlListener = new CommandControlListener(context);
        context.commands().register("hydroxide", new HydroxideCommand(context, commandControlListener));
        context.commands().register("editlocale", new EditLocaleCommand(context).command());
        context.commands().register("help", new HelpCommand(context).command());
        Bukkit.getPluginManager().registerEvents(commandControlListener, context.plugin());
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (commandControlListener != null) {
            commandControlListener.saveCooldowns();
            commandControlListener.cancelAllWarmups();
            HandlerList.unregisterAll(commandControlListener);
        }
    }
}
