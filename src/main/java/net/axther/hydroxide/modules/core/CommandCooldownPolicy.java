package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class CommandCooldownPolicy {

    static final String BYPASS_PERMISSION = "hydroxide.command.cooldown.bypass";

    private final Map<String, Cooldown> cooldowns;

    CommandCooldownPolicy(Map<String, Duration> cooldowns) {
        Map<String, Cooldown> normalized = new LinkedHashMap<>();
        cooldowns.forEach((command, cooldown) -> {
            String key = normalize(command);
            if (!key.isBlank() && cooldown != null && !cooldown.isNegative() && !cooldown.isZero()) {
                normalized.put(key, new Cooldown(key, cooldown));
            }
        });
        this.cooldowns = Map.copyOf(normalized);
    }

    private CommandCooldownPolicy(List<Cooldown> cooldowns) {
        Map<String, Cooldown> normalized = new LinkedHashMap<>();
        for (Cooldown cooldown : cooldowns) {
            String key = normalize(cooldown.key());
            if (!key.isBlank() && (cooldown.oneUse() || (!cooldown.duration().isNegative() && !cooldown.duration().isZero()))) {
                normalized.put(key, new Cooldown(key, cooldown.duration(), cooldown.oneUse()));
            }
        }
        this.cooldowns = Map.copyOf(normalized);
    }

    static CommandCooldownPolicy from(ConfigurationSection section) {
        if (section == null) {
            return new CommandCooldownPolicy(Map.of());
        }
        List<Cooldown> cooldowns = new java.util.ArrayList<>();
        for (String key : section.getKeys(false)) {
            double seconds = section.getDouble(key, 0.0D);
            if (seconds == -1.0D) {
                cooldowns.add(Cooldown.oneUse(key));
            } else if (seconds > 0.0D && !Double.isNaN(seconds) && !Double.isInfinite(seconds)) {
                cooldowns.add(new Cooldown(key, Duration.ofMillis(Math.round(seconds * 1000.0D))));
            }
        }
        return new CommandCooldownPolicy(cooldowns);
    }

    Optional<Cooldown> cooldown(String rawCommand, Predicate<String> hasPermission) {
        if (hasPermission.test(BYPASS_PERMISSION)) {
            return Optional.empty();
        }
        List<String> tokens = commandTokens(rawCommand);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        return candidateKeys(tokens).stream()
                .filter(cooldowns::containsKey)
                .findFirst()
                .map(cooldowns::get);
    }

    private List<String> candidateKeys(List<String> tokens) {
        return java.util.stream.IntStream.rangeClosed(1, Math.min(tokens.size(), 4))
                .mapToObj(length -> String.join("-", tokens.subList(0, length)))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    private static List<String> commandTokens(String rawCommand) {
        String trimmed = rawCommand == null ? "" : rawCommand.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(trimmed.split("\\s+"))
                .map(CommandCooldownPolicy::normalize)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toList());
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    record Cooldown(String key, Duration duration, boolean oneUse) {
        Cooldown(String key, Duration duration) {
            this(key, duration, false);
        }

        static Cooldown oneUse(String key) {
            return new Cooldown(key, Duration.ZERO, true);
        }
    }
}
