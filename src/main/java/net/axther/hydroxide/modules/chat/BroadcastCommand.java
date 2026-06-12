package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class BroadcastCommand implements CommandExecutor {

    private final HydroxideContext context;

    public BroadcastCommand(HydroxideContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.broadcast")) {
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <message>");
            return true;
        }

        String format = context.plugin().getConfig().getString("chat.broadcast-format",
                "<#FFB000><bold>Broadcast</bold> <dark_gray>> <white>{message}");
        String message = CommandUtils.joinArgs(args, 0);
        Component component = context.text().format(format.replace("{message}", message));
        Bukkit.getServer().sendMessage(component);
        return true;
    }
}
