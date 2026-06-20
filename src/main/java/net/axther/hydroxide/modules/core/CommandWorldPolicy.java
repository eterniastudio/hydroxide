package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class CommandWorldPolicy {

    static final String BYPASS_PERMISSION = "hydroxide.command.world-restriction.bypass";

    private final Map<String, Rule> rules;

    CommandWorldPolicy(Map<String, Rule> rules) {
        Map<String, Rule> normalized = new LinkedHashMap<>();
        rules.forEach((command, rule) -> {
            String key = normalize(command);
            Rule cleanRule = normalizeRule(rule);
            if (!key.isBlank() && cleanRule.active()) {
                normalized.put(key, cleanRule);
            }
        });
        this.rules = Map.copyOf(normalized);
    }

    static CommandWorldPolicy from(ConfigurationSection section) {
        if (section == null) {
            return new CommandWorldPolicy(Map.of());
        }
        Map<String, Rule> rules = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection ruleSection = section.getConfigurationSection(key);
            if (ruleSection == null) {
                if (section.isList(key)) {
                    rules.put(key, new Rule(Set.copyOf(section.getStringList(key)), Set.of()));
                }
                continue;
            }
            rules.put(key, new Rule(
                    Set.copyOf(ruleSection.getStringList("allowed-worlds")),
                    Set.copyOf(ruleSection.getStringList("blocked-worlds"))
            ));
        }
        return new CommandWorldPolicy(rules);
    }

    Optional<Restriction> restriction(String rawCommand, String worldName, Predicate<String> hasPermission) {
        if (hasPermission.test(BYPASS_PERMISSION)) {
            return Optional.empty();
        }
        String world = normalizeWorld(worldName);
        if (world.isBlank()) {
            return Optional.empty();
        }
        List<String> tokens = commandTokens(rawCommand);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        return candidateKeys(tokens).stream()
                .filter(rules::containsKey)
                .findFirst()
                .filter(key -> !rules.get(key).allows(world))
                .map(key -> new Restriction(key, world));
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
                .map(CommandWorldPolicy::normalize)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toList());
    }

    private static Rule normalizeRule(Rule rule) {
        if (rule == null) {
            return new Rule(Set.of(), Set.of());
        }
        return new Rule(normalizeWorlds(rule.allowedWorlds()), normalizeWorlds(rule.blockedWorlds()));
    }

    private static Set<String> normalizeWorlds(Set<String> worlds) {
        if (worlds == null) {
            return Set.of();
        }
        return worlds.stream()
                .map(CommandWorldPolicy::normalizeWorld)
                .filter(world -> !world.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeWorld(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    record Rule(Set<String> allowedWorlds, Set<String> blockedWorlds) {
        boolean active() {
            return !allowedWorlds.isEmpty() || !blockedWorlds.isEmpty();
        }

        boolean allows(String world) {
            if (blockedWorlds.contains("*") || blockedWorlds.contains(world)) {
                return false;
            }
            return allowedWorlds.isEmpty() || allowedWorlds.contains("*") || allowedWorlds.contains(world);
        }
    }

    record Restriction(String key, String world) {
    }
}
