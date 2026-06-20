package net.axther.hydroxide.modules.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class TemporaryModeCommandParser {

    private TemporaryModeCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean silent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                values.add(arg);
            }
        }
        if (values.isEmpty() || values.size() > 2) {
            return Optional.empty();
        }
        if (values.size() == 1) {
            return Optional.of(new Request(values.getFirst(), Optional.empty(), false, false, silent));
        }
        String rawDuration = values.get(1);
        boolean additive = rawDuration.startsWith("+");
        String durationValue = additive ? rawDuration.substring(1) : rawDuration;
        try {
            long seconds = Long.parseLong(durationValue);
            if (seconds < 0L || (additive && seconds == 0L)) {
                return Optional.empty();
            }
            return Optional.of(new Request(values.getFirst(), Optional.of(seconds), seconds == 0L, additive, silent));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Request(String targetName,
                   Optional<Long> durationSeconds,
                   boolean indefinite,
                   boolean additive,
                   boolean silent) {
    }
}
