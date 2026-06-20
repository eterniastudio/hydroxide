package net.axther.hydroxide.modules.admin;

import net.axther.hydroxide.commands.framework.CommandArgumentParser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class AdminFireCommandParser {

    private static final int DEFAULT_BURN_SECONDS = 10;
    private static final int MAX_FIRE_SECONDS = Integer.MAX_VALUE / 20;
    private static final CommandArgumentParser ARGUMENTS = new CommandArgumentParser();

    private AdminFireCommandParser() {
    }

    static Optional<BurnRequest> parseBurn(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean silent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                values.add(arg);
            }
        }
        if (values.isEmpty() || values.size() > 2 || values.getFirst().isBlank()) {
            return Optional.empty();
        }

        int seconds = DEFAULT_BURN_SECONDS;
        if (values.size() == 2) {
            Optional<Integer> parsedSeconds = seconds(values.get(1));
            if (parsedSeconds.isEmpty()) {
                return Optional.empty();
            }
            seconds = parsedSeconds.orElseThrow();
        }
        return Optional.of(new BurnRequest(values.getFirst(), seconds, silent));
    }

    static Optional<ExtinguishRequest> parseExtinguish(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean silent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (!arg.startsWith("-")) {
                values.add(arg);
            } else {
                return Optional.empty();
            }
        }
        if (values.size() > 1 || (!values.isEmpty() && values.getFirst().isBlank())) {
            return Optional.empty();
        }
        return Optional.of(new ExtinguishRequest(values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst()), silent));
    }

    static Optional<FireballRequest> parseFireball(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean silent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (!arg.startsWith("-")) {
                values.add(arg);
            } else {
                return Optional.empty();
            }
        }
        if (values.size() > 2 || values.stream().anyMatch(String::isBlank)) {
            return Optional.empty();
        }
        FireballType type = FireballType.SMALL;
        Optional<String> targetName = Optional.empty();
        if (values.size() == 1) {
            Optional<FireballType> parsedType = FireballType.parse(values.getFirst());
            if (parsedType.isPresent()) {
                type = parsedType.orElseThrow();
            } else {
                targetName = Optional.of(values.getFirst());
            }
        } else if (values.size() == 2) {
            Optional<FireballType> parsedType = FireballType.parse(values.getFirst());
            if (parsedType.isEmpty()) {
                return Optional.empty();
            }
            type = parsedType.orElseThrow();
            targetName = Optional.of(values.get(1));
        }
        return Optional.of(new FireballRequest(type, targetName, silent));
    }

    private static Optional<Integer> seconds(String value) {
        try {
            int plainSeconds = Integer.parseInt(value);
            return plainSeconds > 0 && plainSeconds <= MAX_FIRE_SECONDS ? Optional.of(plainSeconds) : Optional.empty();
        } catch (NumberFormatException ignored) {
            Optional<Duration> duration = ARGUMENTS.duration(value)
                    .filter(parsed -> !parsed.isZero() && !parsed.isNegative());
            if (duration.isEmpty() || duration.orElseThrow().getSeconds() > MAX_FIRE_SECONDS) {
                return Optional.empty();
            }
            return Optional.of((int) duration.orElseThrow().getSeconds());
        }
    }

    record BurnRequest(String targetName, int seconds, boolean silent) {
    }

    record ExtinguishRequest(Optional<String> targetName, boolean silent) {
    }

    record FireballRequest(FireballType type, Optional<String> targetName, boolean silent) {
    }

    enum FireballType {
        SMALL,
        LARGE,
        DRAGON;

        static Optional<FireballType> parse(String input) {
            return switch (input.toLowerCase()) {
                case "small", "s" -> Optional.of(SMALL);
                case "large", "big", "l" -> Optional.of(LARGE);
                case "dragon", "ender", "d" -> Optional.of(DRAGON);
                default -> Optional.empty();
            };
        }
    }
}
