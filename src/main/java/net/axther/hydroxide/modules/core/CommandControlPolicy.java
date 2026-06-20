package net.axther.hydroxide.modules.core;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class CommandControlPolicy {

    static final String BYPASS_PERMISSION = "hydroxide.command.disabled.bypass";

    private final Set<String> disabledCommands;

    CommandControlPolicy(List<String> disabledCommands) {
        this.disabledCommands = disabledCommands.stream()
                .map(CommandControlPolicy::normalizeLabel)
                .filter(label -> !label.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    boolean blocked(String rawCommand, Predicate<String> hasPermission) {
        if (hasPermission.test(BYPASS_PERMISSION)) {
            return false;
        }
        return disabledCommands.contains(commandLabel(rawCommand));
    }

    private static String commandLabel(String rawCommand) {
        String trimmed = rawCommand == null ? "" : rawCommand.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int firstSpace = trimmed.indexOf(' ');
        String label = firstSpace >= 0 ? trimmed.substring(0, firstSpace) : trimmed;
        return normalizeLabel(label);
    }

    private static String normalizeLabel(String label) {
        String normalized = label == null ? "" : label.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
