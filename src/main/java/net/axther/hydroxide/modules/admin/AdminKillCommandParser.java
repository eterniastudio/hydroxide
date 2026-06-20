package net.axther.hydroxide.modules.admin;

import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AdminKillCommandParser {

    private AdminKillCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.getFirst().isBlank() || args.getFirst().startsWith("-")) {
            return Optional.empty();
        }

        String targetName = args.getFirst();
        boolean force = false;
        boolean silent = false;
        boolean lightning = false;
        Optional<EntityDamageEvent.DamageCause> damageCause = Optional.empty();

        for (int index = 1; index < args.size(); index++) {
            String token = args.get(index).trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            switch (token.toLowerCase(Locale.ROOT)) {
                case "-force" -> force = true;
                case "-s" -> silent = true;
                case "-lightning" -> lightning = true;
                default -> {
                    if (token.startsWith("-") || damageCause.isPresent()) {
                        return Optional.empty();
                    }
                    Optional<EntityDamageEvent.DamageCause> parsedCause = damageCause(token);
                    if (parsedCause.isEmpty()) {
                        return Optional.empty();
                    }
                    damageCause = parsedCause;
                }
            }
        }

        return Optional.of(new Request(targetName, force, silent, lightning, damageCause));
    }

    static Optional<EntityDamageEvent.DamageCause> damageCause(String input) {
        String normalized = input.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        try {
            return Optional.of(EntityDamageEvent.DamageCause.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    record Request(String targetName, boolean force, boolean silent, boolean lightning,
                   Optional<EntityDamageEvent.DamageCause> damageCause) {
    }
}
