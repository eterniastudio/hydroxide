package net.axther.hydroxide.modules.core;

import net.axther.hydroxide.HydroxideContext;
import net.axther.hydroxide.commands.framework.CommandService;
import net.axther.hydroxide.commands.framework.HydroCommand;
import net.axther.hydroxide.modules.HydroModule;
import net.axther.hydroxide.modules.ModuleStatus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class HydroxideCommand implements CommandExecutor, TabCompleter {

    private final HydroxideContext context;
    private final CommandControlListener commandControlListener;
    private final CommandService commands;

    public HydroxideCommand(HydroxideContext context) {
        this(context, null);
    }

    HydroxideCommand(HydroxideContext context, CommandControlListener commandControlListener) {
        this.context = context;
        this.commandControlListener = commandControlListener;
        this.commands = new CommandService(commandTree(), context.messages());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        commands.onCommand(sender, command, label, args);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return commands.onTabComplete(sender, command, alias, args);
    }

    private HydroCommand commandTree() {
        return HydroCommand.builder("hydroxide")
                .permission("hydroxide.command.hydroxide")
                .usage("/{label} [help|modules|reload|messages reload|cooldown]")
                .executor(ctx -> help(ctx.sender(), ctx.label()))
                .child(HydroCommand.builder("help")
                        .executor(ctx -> help(ctx.sender(), ctx.label()))
                        .build())
                .child(HydroCommand.builder("modules")
                        .executor(ctx -> modules(ctx.sender()))
                        .build())
                .child(HydroCommand.builder("cooldown")
                        .permission("hydroxide.command.cooldown.manage")
                        .usage("/{label} cooldown <status|clear>")
                        .executor(ctx -> cooldownUsage(ctx.sender(), ctx.label()))
                        .child(HydroCommand.builder("status")
                                .executor(ctx -> cooldownStatus(ctx.sender()))
                                .build())
                        .child(HydroCommand.builder("clear")
                                .usage("/{label} cooldown clear <player|all> [command]")
                                .completer(ctx -> completeCooldownClear(ctx.arguments()))
                                .executor(this::cooldownClear)
                                .build())
                        .build())
                .child(HydroCommand.builder("reload")
                        .permission("hydroxide.command.reload")
                        .executor(ctx -> reload(ctx.sender()))
                        .build())
                .child(HydroCommand.builder("messages")
                        .usage("/{label} messages reload")
                        .executor(ctx -> context.message(ctx.sender(), "core.messages.reload-usage", Map.of("label", ctx.label())))
                        .child(HydroCommand.builder("reload")
                                .permission("hydroxide.command.messages.reload")
                                .executor(ctx -> messagesReload(ctx.sender()))
                                .build())
                        .build())
                .build();
    }

    private void help(CommandSender sender, String label) {
        context.message(sender, "core.help.header", Map.of("label", label));
        context.message(sender, "core.help.modules", Map.of("label", label));
        context.message(sender, "core.help.reload", Map.of("label", label));
        context.message(sender, "core.help.messages", Map.of("label", label));
        context.message(sender, "core.help.cooldown", Map.of("label", label));
        context.message(sender, "core.help.locale", Map.of("label", label));
    }

    private void modules(CommandSender sender) {
        for (HydroModule module : context.modules().registeredModules()) {
            ModuleStatus status = context.modules().status(module.id());
            String color = status == ModuleStatus.ENABLED ? "<green>" : "<red>";
            context.message(sender, "core.modules.entry", Map.of(
                    "status_color", color,
                    "module", module.id(),
                    "status", status.name().toLowerCase(Locale.ROOT),
                    "description", module.description()
            ));
        }
    }

    private void reload(CommandSender sender) {
        if (!context.requirePermission(sender, "hydroxide.command.reload")) {
            return;
        }
        context.plugin().reloadConfig();
        context.messages().reload();
        context.modules().reloadEnabledModules(context);
        context.message(sender, "core.reload.success", Map.of());
    }

    private void messagesReload(CommandSender sender) {
        context.messages().reload();
        context.message(sender, "core.messages.reload-success", Map.of());
    }

    private void cooldownUsage(CommandSender sender, String label) {
        context.message(sender, "core.cooldown.usage", Map.of("label", label));
    }

    private void cooldownStatus(CommandSender sender) {
        if (!cooldownsAvailable(sender)) {
            return;
        }
        context.message(sender, "core.cooldown.status", Map.of("count", commandControlListener.activeCooldownCount()));
    }

    private void cooldownClear(net.axther.hydroxide.commands.framework.CommandContext ctx) {
        if (!cooldownsAvailable(ctx.sender())) {
            return;
        }
        if (ctx.arguments().isEmpty() || ctx.arguments().size() > 2) {
            cooldownUsage(ctx.sender(), ctx.label());
            return;
        }
        String scope = ctx.argument(0);
        String commandKey = ctx.arguments().size() == 2 ? normalizeCooldownKey(ctx.argument(1)) : "";
        String commandName = commandKey.isBlank() ? "*" : commandKey;
        if (scope.equalsIgnoreCase("all")) {
            int cleared = commandControlListener.clearAllCooldowns(commandKey);
            context.message(ctx.sender(), "core.cooldown.cleared-all", Map.of(
                    "count", cleared,
                    "command", commandName
            ));
            return;
        }
        Optional<CooldownTarget> target = cooldownTarget(scope, ctx);
        if (target.isEmpty()) {
            context.message(ctx.sender(), "core.cooldown.unknown-player", Map.of("target", scope));
            return;
        }
        CooldownTarget cooldownTarget = target.orElseThrow();
        int cleared = commandControlListener.clearCooldowns(cooldownTarget.playerId(), commandKey);
        context.message(ctx.sender(), "core.cooldown.cleared-player", Map.of(
                "count", cleared,
                "target", cooldownTarget.name(),
                "command", commandName
        ));
    }

    private boolean cooldownsAvailable(CommandSender sender) {
        if (commandControlListener != null) {
            return true;
        }
        context.message(sender, "core.cooldown.unavailable", Map.of());
        return false;
    }

    private List<String> completeCooldownClear(List<String> arguments) {
        if (arguments.size() <= 1) {
            String prefix = arguments.isEmpty() ? "" : arguments.getFirst().toLowerCase(Locale.ROOT);
            List<String> candidates = new ArrayList<>();
            candidates.add("all");
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(candidates::add);
            return candidates.stream()
                    .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        if (arguments.size() == 2) {
            String prefix = arguments.get(1).toLowerCase(Locale.ROOT);
            var section = context.plugin().getConfig().getConfigurationSection("command-control.command-cooldowns");
            if (section == null) {
                return List.of();
            }
            return section.getKeys(false).stream()
                    .map(this::normalizeCooldownKey)
                    .filter(key -> key.startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        return List.of();
    }

    private Optional<CooldownTarget> cooldownTarget(String input, net.axther.hydroxide.commands.framework.CommandContext ctx) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return Optional.of(new CooldownTarget(online.getUniqueId(), online.getName()));
        }
        Optional<UUID> uuid = ctx.parser().uuid(input);
        if (uuid.isPresent()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid.orElseThrow());
            return Optional.of(new CooldownTarget(offline.getUniqueId(), fallbackName(offline)));
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(input);
        if (cached == null) {
            return Optional.empty();
        }
        return Optional.of(new CooldownTarget(cached.getUniqueId(), fallbackName(cached)));
    }

    private String normalizeCooldownKey(String commandKey) {
        String normalized = commandKey == null ? "" : commandKey.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private String fallbackName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private record CooldownTarget(UUID playerId, String name) {
    }
}
