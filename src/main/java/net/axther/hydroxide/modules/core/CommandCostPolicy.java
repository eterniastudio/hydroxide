package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class CommandCostPolicy {

    static final String BYPASS_PERMISSION = "hydroxide.command.cost.bypass";

    private final Map<String, Double> costs;

    CommandCostPolicy(Map<String, Double> costs) {
        Map<String, Double> normalized = new LinkedHashMap<>();
        costs.forEach((command, cost) -> {
            String key = normalize(command);
            if (!key.isBlank() && validCost(cost)) {
                normalized.put(key, cost);
            }
        });
        this.costs = Map.copyOf(normalized);
    }

    static CommandCostPolicy from(ConfigurationSection section) {
        if (section == null) {
            return new CommandCostPolicy(Map.of());
        }
        Map<String, Double> costs = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            costs.put(key, section.getDouble(key, 0.0D));
        }
        return new CommandCostPolicy(costs);
    }

    Optional<Cost> cost(String rawCommand, Predicate<String> hasPermission) {
        if (hasPermission.test(BYPASS_PERMISSION)) {
            return Optional.empty();
        }
        List<String> tokens = commandTokens(rawCommand);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        return candidateKeys(tokens).stream()
                .filter(costs::containsKey)
                .findFirst()
                .map(key -> new Cost(key, costs.get(key)));
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
                .map(CommandCostPolicy::normalize)
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

    private static boolean validCost(double cost) {
        return cost > 0.0D
                && !Double.isNaN(cost)
                && !Double.isInfinite(cost)
                && BigDecimal.valueOf(cost).scale() <= 2;
    }

    record Cost(String key, double amount) {
    }
}
