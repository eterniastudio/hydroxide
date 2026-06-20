package net.axther.hydroxide.modules.jail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JailCommandParser {

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([smhdwMy])");
    private static final Pattern CELL_ID = Pattern.compile("\\d+");

    private JailCommandParser() {
    }

    static Optional<Request> parse(List<String> args, List<String> knownJails, Duration defaultDuration) {
        if (args.isEmpty() || defaultDuration == null || !defaultDuration.isPositive()) {
            return Optional.empty();
        }
        List<String> structural = new ArrayList<>();
        List<String> reasonParts = new ArrayList<>();
        boolean silent = false;
        boolean collectingReason = false;
        for (String arg : args.subList(1, args.size())) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
                continue;
            }
            if (startsReason(arg)) {
                collectingReason = true;
                String firstReasonPart = arg.substring(2);
                if (!firstReasonPart.isBlank()) {
                    reasonParts.add(firstReasonPart);
                }
                continue;
            }
            if (collectingReason) {
                reasonParts.add(arg);
            } else {
                structural.add(arg);
            }
        }

        ParsedStructural parsed = parseStructural(structural, knownJails, defaultDuration).orElse(null);
        if (parsed == null) {
            return Optional.empty();
        }
        if (reasonParts.isEmpty() && !parsed.trailingReason().isEmpty()) {
            reasonParts.addAll(parsed.trailingReason());
        }
        String reason = String.join(" ", reasonParts).trim();
        return Optional.of(new Request(
                args.getFirst(),
                parsed.jailName(),
                parsed.cellId(),
                parsed.duration(),
                reason.isEmpty() ? Optional.empty() : Optional.of(reason),
                silent
        ));
    }

    static Optional<Duration> parseDurationHint(String input) {
        return parseDuration(input);
    }

    private static Optional<ParsedStructural> parseStructural(List<String> values, List<String> knownJails, Duration defaultDuration) {
        if (values.isEmpty()) {
            return Optional.of(new ParsedStructural(Optional.empty(), Optional.empty(), defaultDuration, List.of()));
        }

        Optional<Duration> firstDuration = parseDuration(values.getFirst());
        if (firstDuration.isPresent()) {
            return parseDurationFirst(values, knownJails, firstDuration.get());
        }
        return parseJailFirst(values, knownJails, defaultDuration);
    }

    private static Optional<ParsedStructural> parseDurationFirst(List<String> values, List<String> knownJails, Duration duration) {
        Optional<String> jailName = Optional.empty();
        Optional<String> cellId = Optional.empty();
        int index = 1;
        if (index < values.size()) {
            jailName = Optional.of(normalizeJailName(values.get(index), knownJails));
            index++;
        }
        if (index < values.size() && isCellId(values.get(index))) {
            cellId = Optional.of(values.get(index));
            index++;
        }
        return Optional.of(new ParsedStructural(jailName, cellId, duration, values.subList(index, values.size())));
    }

    private static Optional<ParsedStructural> parseJailFirst(List<String> values, List<String> knownJails, Duration defaultDuration) {
        Optional<String> jailName = Optional.of(normalizeJailName(values.getFirst(), knownJails));
        Optional<String> cellId = Optional.empty();
        Duration duration = defaultDuration;
        int index = 1;
        if (index < values.size()) {
            Optional<Duration> parsedDuration = parseDuration(values.get(index));
            if (parsedDuration.isPresent()) {
                duration = parsedDuration.get();
                index++;
            } else if (isCellId(values.get(index))) {
                cellId = Optional.of(values.get(index));
                index++;
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new ParsedStructural(jailName, cellId, duration, values.subList(index, values.size())));
    }

    private static Optional<Duration> parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        if (input.chars().allMatch(Character::isDigit)) {
            return positiveDuration(() -> Duration.ofSeconds(Long.parseLong(input)));
        }

        Matcher matcher = DURATION_PART.matcher(input);
        int consumed = 0;
        Duration duration = Duration.ZERO;
        while (matcher.find()) {
            if (matcher.start() != consumed) {
                return Optional.empty();
            }
            long amount = Long.parseLong(matcher.group(1));
            duration = addDurationPart(duration, amount, matcher.group(2));
            consumed = matcher.end();
        }
        if (consumed != input.length()) {
            return Optional.empty();
        }
        Duration parsed = duration;
        return positiveDuration(() -> parsed);
    }

    private static Duration addDurationPart(Duration duration, long amount, String unit) {
        return switch (unit) {
            case "s" -> duration.plusSeconds(amount);
            case "m" -> duration.plusMinutes(amount);
            case "h" -> duration.plusHours(amount);
            case "d" -> duration.plusDays(amount);
            case "w" -> duration.plusDays(amount * 7L);
            case "M" -> duration.plusDays(amount * 30L);
            case "y" -> duration.plusDays(amount * 365L);
            default -> duration;
        };
    }

    private static Optional<Duration> positiveDuration(DurationSupplier supplier) {
        try {
            Duration duration = supplier.get();
            return duration.isPositive() ? Optional.of(duration) : Optional.empty();
        } catch (ArithmeticException | NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static boolean startsReason(String input) {
        return input.regionMatches(true, 0, "r:", 0, 2);
    }

    private static boolean isCellId(String input) {
        return CELL_ID.matcher(input).matches();
    }

    private static String normalizeJailName(String input, List<String> knownJails) {
        return knownJails.stream()
                .filter(known -> known.equalsIgnoreCase(input))
                .findFirst()
                .orElse(input.toLowerCase(Locale.ROOT));
    }

    private record ParsedStructural(Optional<String> jailName, Optional<String> cellId,
                                    Duration duration, List<String> trailingReason) {
    }

    private interface DurationSupplier {
        Duration get();
    }

    record Request(String targetName, Optional<String> jailName, Optional<String> cellId,
                   Duration duration, Optional<String> reason, boolean silent) {
    }
}
