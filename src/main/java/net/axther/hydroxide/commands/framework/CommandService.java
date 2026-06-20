package net.axther.hydroxide.commands.framework;

import net.axther.hydroxide.messages.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

public final class CommandService implements CommandExecutor, TabCompleter {

    private final HydroCommand root;
    private final MessageService messages;
    private final LongSupplier clockMillis;
    private final CommandArgumentParser parser = new CommandArgumentParser();
    private final Map<String, Long> cooldowns = new HashMap<>();

    public CommandService(HydroCommand root, MessageService messages) {
        this(root, messages, System::currentTimeMillis);
    }

    public CommandService(HydroCommand root, MessageService messages, LongSupplier clockMillis) {
        this.root = root;
        this.messages = messages;
        this.clockMillis = clockMillis;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        execute(CommandActor.bukkit(sender), label, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return complete(CommandActor.bukkit(sender), alias, args);
    }

    public void execute(CommandActor actor, String label, String[] args) {
        Resolution resolution = resolve(args);
        for (HydroCommand command : resolution.path()) {
            if (!allowed(actor, command)) {
                return;
            }
        }
        HydroCommand command = resolution.command();
        if (command.playerOnly() && !actor.isPlayer()) {
            actor.send(messages.prefixedComponent("validation.player-only", Map.of("usage", command.usage())));
            return;
        }
        if (command.consoleOnly() && actor.isPlayer()) {
            actor.send(messages.prefixedComponent("validation.console-only", Map.of("usage", command.usage())));
            return;
        }
        if (!consumeCooldown(actor, command)) {
            return;
        }
        command.executor().ifPresentOrElse(
                executor -> executor.execute(new CommandContext(actor, messages, label, resolution.arguments(), parser)),
                () -> actor.send(messages.prefixedComponent("validation.usage", Map.of("usage", usage(label, command))))
        );
    }

    public List<String> complete(CommandActor actor, String label, String[] args) {
        if (args.length <= 1) {
            if (root.children().isEmpty()) {
                return root.completer()
                        .map(completer -> completer.complete(new CommandContext(actor, messages, label, List.of(args), parser)))
                        .orElse(List.of());
            }
            String prefix = args.length == 0 ? "" : args[0];
            return root.children().stream()
                    .filter(command -> command.name().startsWith(prefix.toLowerCase(Locale.ROOT)))
                    .filter(command -> visible(actor, command))
                    .map(HydroCommand::name)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
        CompletionResolution resolution = resolveCompletion(args);
        if (resolution.command().children().isEmpty()) {
            return resolution.command().completer()
                    .map(completer -> completer.complete(new CommandContext(actor, messages, label, resolution.arguments(), parser)))
                    .orElse(List.of());
        }
        String prefix = resolution.arguments().isEmpty() ? "" : resolution.arguments().get(0).toLowerCase(Locale.ROOT);
        return resolution.command().children().stream()
                .filter(command -> command.name().startsWith(prefix))
                .filter(command -> visible(actor, command))
                .map(HydroCommand::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean allowed(CommandActor actor, HydroCommand command) {
        if (command.permission().isBlank() || actor.hasPermission(command.permission())) {
            return true;
        }
        actor.send(messages.prefixedComponent("validation.no-permission", Map.of("permission", command.permission())));
        return false;
    }

    private boolean visible(CommandActor actor, HydroCommand command) {
        return command.permission().isBlank() || actor.hasPermission(command.permission());
    }

    private boolean consumeCooldown(CommandActor actor, HydroCommand command) {
        Duration cooldown = command.cooldown();
        if (cooldown.isZero() || cooldown.isNegative()) {
            return true;
        }
        String key = actor.name() + ":" + command.name();
        long now = clockMillis.getAsLong();
        long readyAt = cooldowns.getOrDefault(key, 0L);
        if (readyAt > now) {
            actor.send(messages.prefixedComponent("validation.cooldown", Map.of("remaining", formatRemaining(readyAt - now))));
            return false;
        }
        cooldowns.put(key, now + cooldown.toMillis());
        return true;
    }

    private String formatRemaining(long millis) {
        long seconds = Math.max(1L, (long) Math.ceil(millis / 1000.0D));
        return seconds + "s";
    }

    private String usage(String label, HydroCommand command) {
        if (!command.usage().isBlank()) {
            return command.usage().replace("{label}", label);
        }
        return "/" + label;
    }

    private Resolution resolve(String[] args) {
        HydroCommand current = root;
        List<HydroCommand> path = new ArrayList<>(List.of(root));
        List<String> remaining = new ArrayList<>(Arrays.asList(args));
        while (!remaining.isEmpty()) {
            String candidate = remaining.get(0);
            var child = current.child(candidate);
            if (child.isEmpty()) {
                break;
            }
            current = child.get();
            path.add(current);
            remaining.remove(0);
        }
        return new Resolution(current, path, remaining);
    }

    private CompletionResolution resolveCompletion(String[] args) {
        HydroCommand current = root;
        List<String> remaining = new ArrayList<>(Arrays.asList(args));
        while (remaining.size() > 1) {
            String candidate = remaining.get(0);
            var child = current.child(candidate);
            if (child.isEmpty()) {
                break;
            }
            current = child.get();
            remaining.remove(0);
        }
        return new CompletionResolution(current, remaining);
    }

    private record Resolution(HydroCommand command, List<HydroCommand> path, List<String> arguments) {
    }

    private record CompletionResolution(HydroCommand command, List<String> arguments) {
    }
}
