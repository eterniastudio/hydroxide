package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MessageCommands implements CommandExecutor, TabCompleter {

    private final HydroxideContext context;
    private final Map<UUID, UUID> replies = new HashMap<>();

    public MessageCommands(HydroxideContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "message" -> message(sender, label, args);
            case "reply" -> reply(sender, label, args);
            default -> true;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("message") && args.length == 1) {
            return CommandUtils.matching(args[0], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }

    private boolean message(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.message")) {
            return true;
        }
        if (args.length < 2) {
            context.send(sender, "<red>Usage: /" + label + " <player> <message>");
            return true;
        }
        Player target = CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null) {
            context.send(sender, "<red>That player is not online.");
            return true;
        }
        deliver(sender, target, CommandUtils.joinArgs(args, 1));
        return true;
    }

    private boolean reply(CommandSender sender, String label, String[] args) {
        if (!context.requirePermission(sender, "hydroxide.command.reply")) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            context.send(sender, "<red>Only players can use /" + label + ".");
            return true;
        }
        if (args.length == 0) {
            context.send(sender, "<red>Usage: /" + label + " <message>");
            return true;
        }
        UUID targetId = replies.get(player.getUniqueId());
        Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
        if (target == null) {
            context.send(sender, "<red>You do not have anyone to reply to.");
            return true;
        }
        deliver(sender, target, CommandUtils.joinArgs(args, 0));
        return true;
    }

    private void deliver(CommandSender sender, Player target, String message) {
        String senderName = sender instanceof Player player ? player.getName() : "Console";
        String formattedMessage = context.text().plain(context.text().format(message));
        target.sendMessage(context.text().format("<dark_gray>[<#44CCFF>" + senderName + " <gray>-> <#44CCFF>you<dark_gray>] <white>" + formattedMessage));
        context.send(sender, "<dark_gray>[<#44CCFF>you <gray>-> <#44CCFF>" + target.getName() + "<dark_gray>] <white>" + formattedMessage);
        if (sender instanceof Player player) {
            replies.put(player.getUniqueId(), target.getUniqueId());
            replies.put(target.getUniqueId(), player.getUniqueId());
        }
    }
}
