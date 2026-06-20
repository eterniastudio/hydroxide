package net.axther.hydroxide.modules.teleport;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import java.util.List;

public final class TeleportModule implements HydroModule {

    private TeleportCommands commands;

    @Override
    public String id() {
        return "teleport";
    }

    @Override
    public String displayName() {
        return "Teleport";
    }

    @Override
    public String description() {
        return "Spawn, homes, warps, back, and TPA request workflows.";
    }

    @Override
    public List<String> dependencies() {
        return List.of("core");
    }

    @Override
    public void onEnable(HydroxideContext context) {
        commands = new TeleportCommands(context);
        Bukkit.getPluginManager().registerEvents(commands, context.plugin());
        for (String command : TeleportCommandCatalog.commands()) {
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
