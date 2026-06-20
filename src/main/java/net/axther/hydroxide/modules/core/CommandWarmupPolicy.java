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

final class CommandWarmupPolicy {

    static final String BYPASS_PERMISSION = "hydroxide.command.warmup.bypass";

    private final Map<String, Duration> warmups;

    CommandWarmupPolicy(Map<String, Duration> warmups) {
        Map<String, Duration> normalized = new LinkedHashMap<>();
        warmups.forEach((command, warmup) -> {
            String key = normalize(command);
            if (!key.isBlank() && warmup != null && !warmup.isNegative() && !warmup.isZero()) {
                normalized.put(key, warmup);
            }
        });
        this.warmups = Map.copyOf(normalized);
    }

    static CommandWarmupPolicy from(ConfigurationSection section) {
        if (section == null) {
            return new CommandWarmupPolicy(Map.of());
        }
        Map<String, Duration> warmups = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            double seconds = section.getDouble(key, 0.0D);
            if (seconds > 0.0D && !Double.isNaN(seconds) && !Double.isInfinite(seconds)) {
                warmups.put(key, Duration.ofMillis(Math.round(seconds * 1000.0D)));
            }
        }
        return new CommandWarmupPolicy(warmups);
    }

    Optional<Warmup> warmup(String rawCommand, Predicate<String> hasPermission) {
        if (hasPermission.test(BYPASS_PERMISSION)) {
            return Optional.empty();
        }
        List<String> tokens = commandTokens(rawCommand);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        return candidateKeys(tokens).stream()
                .filter(warmups::containsKey)
                .findFirst()
                .map(key -> new Warmup(key, warmups.get(key)));
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
                .map(CommandWarmupPolicy::normalize)
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

    record Warmup(String key, Duration duration) {
    }
}
