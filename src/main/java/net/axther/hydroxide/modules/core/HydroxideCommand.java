package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.modules.ModuleStatus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class HydroxideCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "modules", "reload");
    private final HydroxideContext context;

    public HydroxideCommand(HydroxideContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.hydroxide")) {
            return true;
        }

        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "reload" -> reload(sender);
            case "modules" -> modules(sender);
            default -> help(sender, label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        return List.of();
    }

    private void help(CommandSender sender, String label) {
        context.send(sender, "<#44CCFF><bold>Hydroxide</bold> <gray>modern Paper server core");
        context.send(sender, "<gray>/" + label + " modules <dark_gray>- <white>Show loaded modules");
        context.send(sender, "<gray>/" + label + " reload <dark_gray>- <white>Reload config and modules");
    }

    private void modules(CommandSender sender) {
        for (HydroModule module : context.modules().registeredModules()) {
            ModuleStatus status = context.modules().status(module.id());
            String color = status == ModuleStatus.ENABLED ? "<green>" : "<red>";
            context.send(sender, color + module.id() + " <dark_gray>- <gray>" + status.name().toLowerCase(Locale.ROOT)
                    + " <dark_gray>- <white>" + module.description());
        }
    }

    private void reload(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.reload")) {
            return;
        }
        context.plugin().reloadConfig();
        context.modules().reloadEnabledModules(context);
        context.send(sender, "<green>Hydroxide configuration and enabled modules reloaded.");
    }
}
