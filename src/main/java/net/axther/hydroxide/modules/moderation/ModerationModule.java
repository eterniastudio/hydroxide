package net.axther.hydroxide.modules.moderation;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.util.List;

public final class ModerationModule implements HydroModule {

    private ModerationCommands commands;

    @Override
    public String id() {
        return "moderation";
    }

    @Override
    public String displayName() {
        return "Moderation";
    }

    @Override
    public String description() {
        return "Quality-of-life moderation commands: fly, god, heal, feed, speed, gamemode, effects, air, and discipline inspection.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        commands = new ModerationCommands(context);
        Bukkit.getPluginManager().registerEvents(commands, context.plugin());
        for (String command : ModerationCommandCatalog.commands()) {
            context.commands().register(command, commands.command(command));
        }
    }

    @Override
    public void onDisable(HydroxideContext context) {
        if (commands != null) {
            HandlerList.unregisterAll(commands);
        }
    }
}
