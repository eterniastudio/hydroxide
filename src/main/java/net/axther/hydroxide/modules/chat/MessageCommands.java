package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MessageCommands {

    private final HydroxideContext context;
    private final ChatControlStore controls;
    private final PrivateMessageSettings privateMessages;
    private final Map<UUID, UUID> replies = new HashMap<>();

    public MessageCommands(HydroxideContext context, ChatControlStore controls) {
        this.context = context;
        this.controls = controls;
        this.privateMessages = new PrivateMessageSettings(controls, context.services()::playerOptionsService);
    }

    public CommandService messageCommand() {
        return new CommandService(HydroCommand.builder("message")
                .permission("hydroxide.command.message")
                .usage("/{label} <player> <message>")
                .executor(ctx -> message(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .completer(ctx -> ctx.arguments().size() == 1
                        ? CommandUtils.matching(ctx.argument(0), Bukkit.getOnlinePlayers().stream().map(Player::getName).toList())
                        : List.of())
                .build(), context.messages());
    }

    public CommandService replyCommand() {
        return new CommandService(HydroCommand.builder("reply")
                .permission("hydroxide.command.reply")
                .usage("/{label} <message>")
                .playerOnly(true)
                .executor(ctx -> reply(ctx.sender(), ctx.label(), ctx.arguments().toArray(String[]::new)))
                .build(), context.messages());
    }

    public CommandService privateChatCommand() {
        return new CommandService(HydroCommand.builder("chat")
                .permission("hydroxide.command.chat")
                .usage("/{label} [player|off]")
                .playerOnly(true)
                .executor(ctx -> privateChat((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1 ? privateChatCompletions(ctx.argument(0)) : List.of())
                .build(), context.messages());
    }

    public CommandService ignoreCommand() {
        return new CommandService(HydroCommand.builder("ignore")
                .permission("hydroxide.command.ignore")
                .usage("/{label} [player|list|clear]")
                .playerOnly(true)
                .executor(ctx -> ignore((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1 ? ignoreCompletions(ctx.argument(0)) : List.of())
                .build(), context.messages());
    }

    public CommandService msgToggleCommand() {
        return new CommandService(HydroCommand.builder("msgtoggle")
                .permission("hydroxide.command.msgtoggle")
                .usage("/{label} [on|off]")
                .playerOnly(true)
                .executor(ctx -> msgToggle((Player) ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1 ? CommandUtils.matching(ctx.argument(0), List.of("off", "on")) : List.of())
                .build(), context.messages());
    }

    public CommandService socialSpyCommand() {
        return new CommandService(HydroCommand.builder("socialspy")
                .permission("hydroxide.command.socialspy")
                .usage("/{label} [player] [on|off]")
                .executor(ctx -> socialSpy(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> spyToggleCompletions(ctx.arguments()))
                .build(), context.messages());
    }

    public CommandService commandSpyCommand() {
        return new CommandService(HydroCommand.builder("commandspy")
                .permission("hydroxide.command.commandspy")
                .usage("/{label} [player] [on|off]")
                .executor(ctx -> commandSpy(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> spyToggleCompletions(ctx.arguments()))
                .build(), context.messages());
    }

    private List<String> ignoreCompletions(String prefix) {
        List<String> values = new java.util.ArrayList<>(List.of("clear", "list"));
        values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        return CommandUtils.matching(prefix, values);
    }

    private List<String> privateChatCompletions(String prefix) {
        List<String> values = new java.util.ArrayList<>(List.of("off"));
        values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        return CommandUtils.matching(prefix, values);
    }

    private List<String> spyToggleCompletions(List<String> args) {
        if (args.size() == 1) {
            List<String> values = new java.util.ArrayList<>(List.of("off", "on"));
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(args.getFirst(), values);
        }
        if (args.size() == 2) {
            return CommandUtils.matching(args.get(1), List.of("off", "on"));
        }
        return List.of();
    }

    private void message(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            context.message(sender, "chat.message.usage", Map.of("label", label));
            return;
        }
        Player target = CommandUtils.onlinePlayer(args[0]).orElse(null);
        if (target == null) {
            context.message(sender, "chat.message.player-offline", Map.of("target", args[0]));
            return;
        }
        deliver(sender, target, CommandUtils.joinArgs(args, 1));
    }

    private void reply(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        if (args.length == 0) {
            context.message(sender, "chat.reply.usage", Map.of("label", label));
            return;
        }
        UUID targetId = replies.get(player.getUniqueId());
        Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
        if (target == null) {
            context.message(sender, "chat.reply.no-target", Map.of());
            return;
        }
        deliver(sender, target, CommandUtils.joinArgs(args, 0));
    }

    private void ignore(Player player, String label, List<String> args) {
        if (args.isEmpty() || args.getFirst().equalsIgnoreCase("list")) {
            List<String> ignored = controls.ignoredPlayers(player.getUniqueId()).stream()
                    .map(this::knownName)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            context.message(player, ignored.isEmpty() ? "chat.ignore.empty" : "chat.ignore.list",
                    Map.of("players", String.join("<gray>, <white>", ignored)));
            return;
        }
        if (args.getFirst().equalsIgnoreCase("clear")) {
            int count = controls.clearIgnores(player.getUniqueId());
            context.message(player, "chat.ignore.cleared", Map.of("count", count));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args.getFirst());
        if (target.getUniqueId().equals(player.getUniqueId())) {
            context.message(player, "chat.ignore.self-denied", Map.of());
            return;
        }
        if (controls.isIgnored(player.getUniqueId(), target.getUniqueId())) {
            controls.removeIgnore(player.getUniqueId(), target.getUniqueId());
            context.message(player, "chat.ignore.removed", Map.of("target", knownName(target)));
        } else {
            controls.addIgnore(player.getUniqueId(), target.getUniqueId());
            context.message(player, "chat.ignore.added", Map.of("target", knownName(target)));
        }
    }

    private void privateChat(Player player, String label, List<String> args) {
        java.util.Optional<PrivateChatCommandParser.Request> parsed = PrivateChatCommandParser.parse(args);
        if (parsed.isEmpty()) {
            context.message(player, "chat.private-chat.usage", Map.of("label", label));
            return;
        }

        PrivateChatCommandParser.Request request = parsed.orElseThrow();
        switch (request.action()) {
            case STATUS -> privateChatStatus(player);
            case CLEAR -> {
                controls.clearPrivateChatTarget(player.getUniqueId());
                context.message(player, "chat.private-chat.disabled", Map.of());
            }
            case FOCUS -> focusPrivateChat(player, request.targetName().orElseThrow());
        }
    }

    private void privateChatStatus(Player player) {
        java.util.Optional<UUID> targetId = controls.privateChatTarget(player.getUniqueId());
        if (targetId.isEmpty()) {
            context.message(player, "chat.private-chat.status-off", Map.of());
            return;
        }
        context.message(player, "chat.private-chat.status-on", Map.of("target", knownName(targetId.orElseThrow())));
    }

    private void focusPrivateChat(Player player, String targetName) {
        Player target = CommandUtils.onlinePlayer(targetName).orElse(null);
        if (target == null) {
            context.message(player, "chat.private-chat.player-offline", Map.of("target", targetName));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            context.message(player, "chat.private-chat.self-denied", Map.of());
            return;
        }
        if (!privateMessages.enabled(target.getUniqueId()) && !player.hasPermission("hydroxide.command.message.bypass")) {
            context.message(player, "chat.message.target-disabled", Map.of("target", target.getName()));
            return;
        }
        controls.setPrivateChatTarget(player.getUniqueId(), target.getUniqueId());
        context.message(player, "chat.private-chat.enabled", Map.of("target", target.getName()));
    }

    private void socialSpy(CommandSender sender, String label, List<String> args) {
        Player target;
        Boolean requestedState = null;
        if (args.isEmpty()) {
            target = requirePlayer(sender, "/socialspy");
            if (target == null) {
                return;
            }
        } else if (isState(args.getFirst())) {
            target = requirePlayer(sender, "/socialspy");
            if (target == null) {
                return;
            }
            requestedState = parseState(args.getFirst());
        } else {
            target = CommandUtils.onlinePlayer(args.getFirst()).orElse(null);
            if (target == null) {
                context.message(sender, "chat.socialspy.player-offline", Map.of("target", args.getFirst()));
                return;
            }
            if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.socialspy.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.socialspy.others"));
                return;
            }
            if (args.size() > 1) {
                if (!isState(args.get(1))) {
                    context.message(sender, "chat.socialspy.usage", Map.of("label", label));
                    return;
                }
                requestedState = parseState(args.get(1));
            }
        }
        boolean enabled = requestedState == null
                ? !controls.isSocialSpyEnabled(target.getUniqueId())
                : requestedState;
        controls.setSocialSpy(target.getUniqueId(), enabled);
        String key = enabled ? "chat.socialspy.enabled" : "chat.socialspy.disabled";
        if (sender.equals(target)) {
            context.message(sender, key, Map.of());
        } else {
            context.message(sender, "chat.socialspy.updated", Map.of(
                    "target", target.getName(),
                    "state", enabled ? "enabled" : "disabled"
            ));
            context.message(target, key, Map.of());
        }
    }

    private void commandSpy(CommandSender sender, String label, List<String> args) {
        Player target;
        Boolean requestedState = null;
        if (args.isEmpty()) {
            target = requirePlayer(sender, "/commandspy");
            if (target == null) {
                return;
            }
        } else if (isState(args.getFirst())) {
            target = requirePlayer(sender, "/commandspy");
            if (target == null) {
                return;
            }
            requestedState = parseState(args.getFirst());
        } else {
            target = CommandUtils.onlinePlayer(args.getFirst()).orElse(null);
            if (target == null) {
                context.message(sender, "chat.commandspy.player-offline", Map.of("target", args.getFirst()));
                return;
            }
            if (!sender.equals(target) && !sender.hasPermission("hydroxide.command.commandspy.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.commandspy.others"));
                return;
            }
            if (args.size() > 1) {
                if (!isState(args.get(1))) {
                    context.message(sender, "chat.commandspy.usage", Map.of("label", label));
                    return;
                }
                requestedState = parseState(args.get(1));
            }
        }
        boolean enabled = requestedState == null
                ? !controls.isCommandSpyEnabled(target.getUniqueId())
                : requestedState;
        controls.setCommandSpy(target.getUniqueId(), enabled);
        String key = enabled ? "chat.commandspy.enabled" : "chat.commandspy.disabled";
        if (sender.equals(target)) {
            context.message(sender, key, Map.of());
        } else {
            context.message(sender, "chat.commandspy.updated", Map.of(
                    "target", target.getName(),
                    "state", enabled ? "enabled" : "disabled"
            ));
            context.message(target, key, Map.of());
        }
    }

    private void msgToggle(Player player, String label, List<String> args) {
        if (args.size() > 1 || (!args.isEmpty() && !isState(args.getFirst()))) {
            context.message(player, "chat.msgtoggle.usage", Map.of("label", label));
            return;
        }
        boolean enabled = args.isEmpty()
                ? !privateMessages.enabled(player.getUniqueId())
                : parseState(args.getFirst());
        privateMessages.set(player.getUniqueId(), enabled);
        context.message(player, enabled ? "chat.msgtoggle.enabled" : "chat.msgtoggle.disabled", Map.of());
    }

    boolean deliver(CommandSender sender, Player target, String message) {
        UUID senderId = sender instanceof Player player ? player.getUniqueId() : null;
        if (senderId != null && controls.isIgnored(senderId, target.getUniqueId())) {
            context.message(sender, "chat.message.target-ignored", Map.of("target", target.getName()));
            return false;
        }
        if (senderId != null && controls.isIgnored(target.getUniqueId(), senderId)) {
            context.message(sender, "chat.message.ignored-by-target", Map.of("target", target.getName()));
            return false;
        }
        if (!target.getUniqueId().equals(senderId)
                && !privateMessages.enabled(target.getUniqueId())
                && !sender.hasPermission("hydroxide.command.message.bypass")) {
            context.message(sender, "chat.message.target-disabled", Map.of("target", target.getName()));
            return false;
        }
        String senderName = sender instanceof Player player
                ? player.getName()
                : context.text().plain(context.messages().component("chat.message.console-name", Map.of()));
        String formattedMessage = context.text().plain(context.text().format(message));
        target.sendMessage(context.messages().component("chat.message.received", Map.of(
                "player", senderName,
                "message", formattedMessage
        )));
        context.message(sender, "chat.message.sent", Map.of(
                "target", target.getName(),
                "message", formattedMessage
        ));
        if (sender instanceof Player player) {
            replies.put(player.getUniqueId(), target.getUniqueId());
            replies.put(target.getUniqueId(), player.getUniqueId());
        }
        spy(sender, senderName, target, formattedMessage);
        return true;
    }

    private void spy(CommandSender sender, String senderName, Player target, String message) {
        Component component = context.messages().component("chat.socialspy.format", Map.of(
                "player", senderName,
                "target", target.getName(),
                "message", message
        ));
        UUID senderId = sender instanceof Player player ? player.getUniqueId() : null;
        for (UUID spyId : controls.socialSpies()) {
            if (spyId.equals(senderId) || spyId.equals(target.getUniqueId())) {
                continue;
            }
            Player staff = Bukkit.getPlayer(spyId);
            if (staff != null && staff.hasPermission("hydroxide.command.socialspy")) {
                staff.sendMessage(component);
            }
        }
    }

    private Player requirePlayer(CommandSender sender, String usage) {
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "validation.player-only", Map.of("usage", usage));
        return null;
    }

    private boolean isState(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("on") || normalized.equals("off") || normalized.equals("true") || normalized.equals("false");
    }

    private boolean parseState(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("on") || normalized.equals("true");
    }

    private String knownName(UUID playerId) {
        return knownName(Bukkit.getOfflinePlayer(playerId));
    }

    private String knownName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }
}
