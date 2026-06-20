package net.axther.hydroxide.modules.chat;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.CommandUtils;
import net.axther.hydroxide.commands.CompletionUtils;
import net.axther.hydroxide.commands.framework.CommandContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ColorReferenceCommands {

    private final HydroxideContext context;
    private final ChatColorSettings chatColors;

    ColorReferenceCommands(HydroxideContext context, ChatControlStore controls) {
        this.context = context;
        this.chatColors = new ChatColorSettings(controls);
    }

    CommandService chatColorCommand() {
        return new CommandService(HydroCommand.builder("chatcolor")
                .permission("hydroxide.command.chatcolor")
                .usage("/{label} [player] <hex|color|clear>")
                .executor(ctx -> chatColor(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::chatColorCompletions)
                .build(), context.messages());
    }

    CommandService colorsCommand() {
        return new CommandService(HydroCommand.builder("colors")
                .permission("hydroxide.command.colors")
                .usage("/{label}")
                .executor(ctx -> colors(ctx.sender()))
                .build(), context.messages());
    }

    private void chatColor(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            Player player = requirePlayer(sender, label);
            if (player != null) {
                showCurrentChatColor(sender, player);
            }
            return;
        }
        if (args.size() > 2) {
            context.message(sender, "chat.chatcolor.usage", Map.of("label", label));
            return;
        }

        Player target;
        String value;
        if (args.size() == 1) {
            Player onlineTarget = Bukkit.getPlayerExact(args.getFirst());
            if (onlineTarget != null && sender.hasPermission("hydroxide.command.chatcolor.others")) {
                showCurrentChatColor(sender, onlineTarget);
                return;
            }
            target = requirePlayer(sender, label);
            if (target == null) {
                return;
            }
            value = args.getFirst();
        } else {
            if (!sender.hasPermission("hydroxide.command.chatcolor.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.chatcolor.others"));
                return;
            }
            target = Bukkit.getPlayerExact(args.getFirst());
            if (target == null) {
                context.message(sender, "chat.chatcolor.player-offline", Map.of("target", args.getFirst()));
                return;
            }
            value = args.get(1);
        }

        if (value.equalsIgnoreCase("clear") || value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("none")) {
            chatColors.clear(target.getUniqueId());
            context.message(sender, "chat.chatcolor.cleared", Map.of("target", target.getName()));
            return;
        }

        ColorPalette.Selection selection = ColorPalette.pick(value).orElse(null);
        if (selection == null) {
            context.message(sender, "chat.chatcolor.invalid", Map.of("value", value));
            return;
        }
        chatColors.set(target.getUniqueId(), selection);
        context.message(sender, "chat.chatcolor.set", chatColorPlaceholders(target, selection));
    }

    private void showCurrentChatColor(CommandSender sender, Player target) {
        ColorPalette.Selection selection = chatColors.selected(target.getUniqueId()).orElse(null);
        if (selection == null) {
            context.message(sender, "chat.chatcolor.none", Map.of("target", target.getName()));
            return;
        }
        context.message(sender, "chat.chatcolor.current", chatColorPlaceholders(target, selection));
    }

    CommandService colorPickerCommand() {
        return new CommandService(HydroCommand.builder("colorpicker")
                .permission("hydroxide.command.colorpicker")
                .usage("/{label} <hex|color>")
                .executor(ctx -> colorPicker(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(this::colorCompletions)
                .build(), context.messages());
    }

    CommandService colorLimitsCommand() {
        return new CommandService(HydroCommand.builder("colorlimits")
                .permission("hydroxide.command.colorlimits")
                .usage("/{label} [player]")
                .executor(ctx -> colorLimits(ctx.sender(), ctx.label(), ctx.arguments()))
                .completer(ctx -> ctx.arguments().size() == 1 ? CompletionUtils.onlinePlayers(ctx.argument(0)) : List.of())
                .build(), context.messages());
    }

    private void colors(CommandSender sender) {
        context.message(sender, "chat.colors.header", Map.of("count", ColorPalette.entries().size()));
        for (ColorPalette.Entry entry : ColorPalette.entries()) {
            context.message(sender, "chat.colors.entry", Map.of(
                    "name", entry.name(),
                    "legacy", entry.legacy(),
                    "hex", entry.hex(),
                    "preview", entry.miniMessage(),
                    "mini", entry.literalMiniMessage()
            ));
        }
    }

    private void colorPicker(CommandSender sender, String label, List<String> args) {
        if (args.size() != 1) {
            context.message(sender, "chat.colorpicker.usage", Map.of("label", label));
            return;
        }
        String input = args.getFirst();
        ColorPalette.Selection selection = ColorPalette.pick(input).orElse(null);
        if (selection == null) {
            context.message(sender, "chat.colorpicker.invalid", Map.of("value", input));
            return;
        }
        context.message(sender, "chat.colorpicker.result", Map.of(
                "input", input,
                "name", selection.closest().name(),
                "legacy", selection.closest().legacy(),
                "hex", selection.hex(),
                "preview", selection.miniMessage(),
                "mini", selection.literalMiniMessage()
        ));
    }

    private void colorLimits(CommandSender sender, String label, List<String> args) {
        if (args.size() > 1) {
            context.message(sender, "chat.colorlimits.usage", Map.of("label", label));
            return;
        }
        CommandSender target = sender;
        String targetName = sender.getName();
        if (args.size() == 1) {
            if (!sender.hasPermission("hydroxide.command.colorlimits.others")) {
                context.message(sender, "validation.no-permission", Map.of("permission", "hydroxide.command.colorlimits.others"));
                return;
            }
            Player player = Bukkit.getPlayerExact(args.getFirst());
            if (player == null) {
                context.message(sender, "chat.colorlimits.player-offline", Map.of("target", args.getFirst()));
                return;
            }
            target = player;
            targetName = player.getName();
        }
        context.message(sender, "chat.colorlimits.result", Map.of(
                "target", targetName,
                "chat_color", state(target.hasPermission("hydroxide.chat.color")),
                "nickname_color", state(target.hasPermission("hydroxide.nickname.color")),
                "nickname_hex", state(target.hasPermission("hydroxide.nickname.hex")),
                "nickname_gradient", state(target.hasPermission("hydroxide.nickname.gradient")),
                "nickname_rainbow", state(target.hasPermission("hydroxide.nickname.rainbow"))
        ));
    }

    private String state(boolean allowed) {
        return context.messages().template(allowed ? "chat.colorlimits.state.allowed" : "chat.colorlimits.state.denied",
                allowed ? "allowed" : "denied");
    }

    private Map<String, String> chatColorPlaceholders(Player target, ColorPalette.Selection selection) {
        return Map.of(
                "target", target.getName(),
                "name", selection.closest().name(),
                "legacy", selection.closest().legacy(),
                "hex", selection.hex(),
                "preview", selection.miniMessage(),
                "mini", selection.literalMiniMessage()
        );
    }

    private Player requirePlayer(CommandSender sender, String label) {
        if (sender instanceof Player player) {
            return player;
        }
        context.message(sender, "validation.player-only", Map.of("usage", "/" + label + " <player> <hex|color|clear>"));
        return null;
    }

    private List<String> chatColorCompletions(CommandContext ctx) {
        if (ctx.arguments().size() == 1) {
            List<String> values = new ArrayList<>(List.of("clear", "none", "reset"));
            values.addAll(ColorPalette.entries().stream().map(ColorPalette.Entry::name).toList());
            values.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return CommandUtils.matching(ctx.argument(0), values);
        }
        if (ctx.arguments().size() == 2) {
            List<String> values = new ArrayList<>(List.of("clear", "none", "reset"));
            values.addAll(ColorPalette.entries().stream().map(ColorPalette.Entry::name).toList());
            values.addAll(ColorPalette.entries().stream().map(ColorPalette.Entry::legacy).toList());
            return CommandUtils.matching(ctx.argument(1), values);
        }
        return List.of();
    }

    private List<String> colorCompletions(CommandContext ctx) {
        if (ctx.arguments().size() != 1) {
            return List.of();
        }
        List<String> values = new ArrayList<>(ColorPalette.entries().stream()
                .map(ColorPalette.Entry::name)
                .toList());
        values.addAll(ColorPalette.entries().stream().map(ColorPalette.Entry::legacy).toList());
        return CommandUtils.matching(ctx.argument(0), values);
    }
}
