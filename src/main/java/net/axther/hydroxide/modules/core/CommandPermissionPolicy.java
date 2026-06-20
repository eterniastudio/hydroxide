package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class CommandPermissionPolicy {

    static final String BYPASS_PERMISSION = "hydroxide.command.permission.bypass";

    private final Map<String, Rule> rules;

    CommandPermissionPolicy(Map<String, Rule> rules) {
        Map<String, Rule> normalized = new LinkedHashMap<>();
        rules.forEach((command, rule) -> {
            String key = normalize(command);
            Rule cleanRule = cleanRule(rule);
            if (!key.isBlank() && !cleanRule.permissions().isEmpty()) {
                normalized.put(key, cleanRule);
            }
        });
        this.rules = Map.copyOf(normalized);
    }

    static CommandPermissionPolicy from(ConfigurationSection section) {
        if (section == null) {
            return new CommandPermissionPolicy(Map.of());
        }
        Map<String, Rule> rules = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection ruleSection = section.getConfigurationSection(key);
            if (ruleSection != null) {
                rules.put(key, new Rule(ruleSection.getStringList("permissions"), Mode.from(ruleSection.getString("mode", "any"))));
            } else if (section.isList(key)) {
                rules.put(key, new Rule(section.getStringList(key), Mode.ANY));
            } else {
                rules.put(key, new Rule(List.of(section.getString(key, "")), Mode.ANY));
            }
        }
        return new CommandPermissionPolicy(rules);
    }

    Optional<MissingPermission> missing(String rawCommand, Predicate<String> hasPermission) {
        if (hasPermission.test(BYPASS_PERMISSION)) {
            return Optional.empty();
        }
        List<String> tokens = commandTokens(rawCommand);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        return candidateKeys(tokens).stream()
                .filter(rules::containsKey)
                .findFirst()
                .filter(key -> !rules.get(key).allows(hasPermission))
                .map(key -> new MissingPermission(key, rules.get(key)));
    }

    private List<String> candidateKeys(List<String> tokens) {
        return java.util.stream.IntStream.rangeClosed(1, Math.min(tokens.size(), 4))
                .mapToObj(length -> String.join("-", tokens.subList(0, length)))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    private static Rule cleanRule(Rule rule) {
        if (rule == null) {
            return new Rule(List.of(), Mode.ANY);
        }
        return new Rule(rule.permissions().stream()
                .map(permission -> permission == null ? "" : permission.trim().toLowerCase(Locale.ROOT))
                .filter(permission -> !permission.isBlank())
                .distinct()
                .toList(), rule.mode() == null ? Mode.ANY : rule.mode());
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
                .map(CommandPermissionPolicy::normalize)
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

    enum Mode {
        ANY,
        ALL;

        static Mode from(String value) {
            return "all".equalsIgnoreCase(value == null ? "" : value.trim()) ? ALL : ANY;
        }
    }

    record Rule(List<String> permissions, Mode mode) {
        boolean allows(Predicate<String> hasPermission) {
            if (mode == Mode.ALL) {
                return permissions.stream().allMatch(hasPermission);
            }
            return permissions.stream().anyMatch(hasPermission);
        }
    }

    record MissingPermission(String key, Rule rule) {
        String permissionList() {
            return String.join(", ", rule.permissions());
        }
    }
}
