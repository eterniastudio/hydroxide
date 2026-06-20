package net.axther.hydroxide.commands.framework;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class HydroCommand {

    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final String usage;
    private final boolean playerOnly;
    private final boolean consoleOnly;
    private final Duration cooldown;
    private final HydroCommandExecutor executor;
    private final HydroTabCompleter completer;
    private final Map<String, HydroCommand> children;

    private HydroCommand(Builder builder) {
        this.name = builder.name;
        this.aliases = List.copyOf(builder.aliases);
        this.permission = builder.permission;
        this.usage = builder.usage;
        this.playerOnly = builder.playerOnly;
        this.consoleOnly = builder.consoleOnly;
        this.cooldown = builder.cooldown;
        this.executor = builder.executor;
        this.completer = builder.completer;
        this.children = Map.copyOf(builder.children);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public List<String> aliases() {
        return aliases;
    }

    public String permission() {
        return permission;
    }

    public String usage() {
        return usage;
    }

    public boolean playerOnly() {
        return playerOnly;
    }

    public boolean consoleOnly() {
        return consoleOnly;
    }

    public Duration cooldown() {
        return cooldown;
    }

    public Optional<HydroCommandExecutor> executor() {
        return Optional.ofNullable(executor);
    }

    public Optional<HydroTabCompleter> completer() {
        return Optional.ofNullable(completer);
    }

    public List<HydroCommand> children() {
        return children.values().stream()
                .distinct()
                .sorted(Comparator.comparing(HydroCommand::name))
                .toList();
    }

    public Optional<HydroCommand> child(String input) {
        return Optional.ofNullable(children.get(input.toLowerCase(Locale.ROOT)));
    }

    @FunctionalInterface
    public interface HydroCommandExecutor {
        void execute(CommandContext context);
    }

    @FunctionalInterface
    public interface HydroTabCompleter {
        List<String> complete(CommandContext context);
    }

    public static final class Builder {
        private final String name;
        private final List<String> aliases = new ArrayList<>();
        private final Map<String, HydroCommand> children = new LinkedHashMap<>();
        private String permission = "";
        private String usage = "";
        private boolean playerOnly;
        private boolean consoleOnly;
        private Duration cooldown = Duration.ZERO;
        private HydroCommandExecutor executor;
        private HydroTabCompleter completer;

        private Builder(String name) {
            this.name = name.toLowerCase(Locale.ROOT);
        }

        public Builder aliases(String... aliases) {
            this.aliases.addAll(List.of(aliases));
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder playerOnly(boolean playerOnly) {
            this.playerOnly = playerOnly;
            return this;
        }

        public Builder consoleOnly(boolean consoleOnly) {
            this.consoleOnly = consoleOnly;
            return this;
        }

        public Builder cooldown(Duration cooldown) {
            this.cooldown = cooldown == null ? Duration.ZERO : cooldown;
            return this;
        }

        public Builder executor(HydroCommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        public Builder completer(HydroTabCompleter completer) {
            this.completer = completer;
            return this;
        }

        public Builder child(HydroCommand child) {
            children.put(child.name(), child);
            for (String alias : child.aliases()) {
                children.put(alias.toLowerCase(Locale.ROOT), child);
            }
            return this;
        }

        public HydroCommand build() {
            return new HydroCommand(this);
        }
    }
}
