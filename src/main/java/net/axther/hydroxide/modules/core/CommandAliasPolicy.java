package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class CommandAliasPolicy {

    private final Map<String, String> aliases;

    CommandAliasPolicy(Map<String, String> aliases) {
        Map<String, String> normalized = new LinkedHashMap<>();
        aliases.forEach((alias, target) -> {
            String key = normalizeLabel(alias);
            String command = cleanTarget(target);
            if (!key.isBlank() && !command.isBlank() && !key.equals(commandLabel(command))) {
                normalized.put(key, command);
            }
        });
        this.aliases = Map.copyOf(normalized);
    }

    static CommandAliasPolicy from(ConfigurationSection section) {
        if (section == null) {
            return new CommandAliasPolicy(Map.of());
        }
        Map<String, String> aliases = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String target = section.getString(key, "");
            if (target != null && !target.isBlank()) {
                aliases.put(key, target);
            }
        }
        return new CommandAliasPolicy(aliases);
    }

    Optional<String> rewrite(String rawCommand, String playerName) {
        ParsedCommand parsed = parse(rawCommand);
        if (parsed.label().isBlank() || !aliases.containsKey(parsed.label())) {
            return Optional.empty();
        }
        String target = aliases.get(parsed.label())
                .replace("{player}", playerName == null ? "" : playerName);
        if (target.contains("{args}")) {
            target = target.replace("{args}", parsed.arguments());
        } else if (!parsed.arguments().isBlank()) {
            target = target + " " + parsed.arguments();
        }
        target = cleanTarget(target);
        if (target.isBlank() || parsed.label().equals(commandLabel(target))) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    private static ParsedCommand parse(String rawCommand) {
        String trimmed = rawCommand == null ? "" : rawCommand.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isBlank()) {
            return new ParsedCommand("", "");
        }
        int firstSpace = trimmed.indexOf(' ');
        String label = firstSpace >= 0 ? trimmed.substring(0, firstSpace) : trimmed;
        String arguments = firstSpace >= 0 ? trimmed.substring(firstSpace + 1).trim() : "";
        return new ParsedCommand(normalizeLabel(label), arguments);
    }

    private static String cleanTarget(String target) {
        String clean = target == null ? "" : target.trim().replaceAll("\\s+", " ");
        if (clean.startsWith("/")) {
            clean = clean.substring(1).trim();
        }
        return clean.isBlank() ? "" : "/" + clean;
    }

    private static String commandLabel(String command) {
        return parse(command).label();
    }

    private static String normalizeLabel(String label) {
        String normalized = label == null ? "" : label.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private record ParsedCommand(String label, String arguments) {
    }
}
