package net.axther.hydroxide.modules.chat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MuteChatCommandParser {

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([smhdw])");
    private MuteChatCommandParser() {
    }

    static Optional<Request> parse(List<String> args, Duration defaultDuration) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Action.ENABLE, Optional.of(defaultDuration), false, ""));
        }

        String first = args.getFirst().toLowerCase(Locale.ROOT);
        if (List.of("status", "check").contains(first)) {
            return args.size() == 1 ? Optional.of(new Request(Action.STATUS, Optional.empty(), false, "")) : Optional.empty();
        }
        if (List.of("off", "clear", "stop", "disable").contains(first)) {
            return args.size() == 1 ? Optional.of(new Request(Action.DISABLE, Optional.empty(), false, "")) : Optional.empty();
        }

        Optional<Duration> duration = parseDuration(first);
        if (duration.isEmpty()) {
            return Optional.empty();
        }

        boolean silent = false;
        List<String> reasonParts = new ArrayList<>();
        for (String arg : args.subList(1, args.size())) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else {
                reasonParts.add(arg);
            }
        }
        String reason = reasonParts.isEmpty() ? "" : String.join(" ", reasonParts);
        return Optional.of(new Request(Action.ENABLE, duration, silent, reason));
    }

    private static Optional<Duration> parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = DURATION_PART.matcher(input.toLowerCase(Locale.ROOT));
        int consumed = 0;
        Duration duration = Duration.ZERO;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                return Optional.empty();
            }
            long amount = Long.parseLong(matcher.group(1));
            duration = duration.plus(switch (matcher.group(2)) {
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                case "w" -> Duration.ofDays(amount * 7L);
                default -> Duration.ZERO;
            });
            consumed = matcher.end();
        }
        if (consumed != input.length() || duration.isZero() || duration.isNegative()) {
            return Optional.empty();
        }
        return Optional.of(duration);
    }

    enum Action {
        ENABLE,
        DISABLE,
        STATUS
    }

    record Request(Action action, Optional<Duration> duration, boolean silent, String reason) {
    }
}
