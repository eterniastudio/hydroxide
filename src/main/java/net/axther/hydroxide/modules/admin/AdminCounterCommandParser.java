package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

final class AdminCounterCommandParser {

    private AdminCounterCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        String actionName = args.getFirst().toLowerCase(java.util.Locale.ROOT);
        return switch (actionName) {
            case "join" -> args.size() == 1 ? Optional.of(new Request(Action.JOIN, OptionalInt.empty(),
                    OptionalDouble.empty(), Optional.empty(), Optional.empty(), false)) : Optional.empty();
            case "leave" -> args.size() == 1 ? Optional.of(new Request(Action.LEAVE, OptionalInt.empty(),
                    OptionalDouble.empty(), Optional.empty(), Optional.empty(), false)) : Optional.empty();
            case "start" -> parseStart(args.subList(1, args.size()));
            default -> Optional.empty();
        };
    }

    private static Optional<Request> parseStart(List<String> args) {
        OptionalInt seconds = OptionalInt.empty();
        OptionalDouble range = OptionalDouble.empty();
        Optional<Center> center = Optional.empty();
        Optional<String> message = Optional.empty();
        boolean force = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-f")) {
                if (force) {
                    return Optional.empty();
                }
                force = true;
            } else if (arg.regionMatches(true, 0, "t:", 0, 2)) {
                if (seconds.isPresent()) {
                    return Optional.empty();
                }
                seconds = parseSeconds(arg.substring(2));
                if (seconds.isEmpty()) {
                    return Optional.empty();
                }
            } else if (arg.regionMatches(true, 0, "r:", 0, 2)) {
                if (range.isPresent()) {
                    return Optional.empty();
                }
                range = parseRange(arg.substring(2));
                if (range.isEmpty()) {
                    return Optional.empty();
                }
            } else if (arg.regionMatches(true, 0, "c:", 0, 2)) {
                if (center.isPresent()) {
                    return Optional.empty();
                }
                center = parseCenter(arg.substring(2));
                if (center.isEmpty()) {
                    return Optional.empty();
                }
            } else if (arg.regionMatches(true, 0, "msg:", 0, 4)) {
                if (message.isPresent()) {
                    return Optional.empty();
                }
                String value = arg.substring(4).replace('_', ' ').trim();
                if (value.isBlank()) {
                    return Optional.empty();
                }
                message = Optional.of(value);
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(new Request(Action.START, seconds, range, center, message, force));
    }

    private static OptionalInt parseSeconds(String input) {
        if (input.isBlank()) {
            return OptionalInt.empty();
        }
        if (input.chars().allMatch(Character::isDigit)) {
            try {
                return positiveSeconds(Long.parseLong(input));
            } catch (NumberFormatException exception) {
                return OptionalInt.empty();
            }
        }
        int index = 0;
        long total = 0L;
        while (index < input.length()) {
            int numberStart = index;
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (numberStart == index || index >= input.length()) {
                return OptionalInt.empty();
            }
            long amount;
            try {
                amount = Long.parseLong(input.substring(numberStart, index));
            } catch (NumberFormatException exception) {
                return OptionalInt.empty();
            }
            char unit = Character.toLowerCase(input.charAt(index++));
            long multiplier = switch (unit) {
                case 's' -> 1L;
                case 'm' -> 60L;
                case 'h' -> 60L * 60L;
                case 'd' -> 60L * 60L * 24L;
                case 'w' -> 60L * 60L * 24L * 7L;
                default -> -1L;
            };
            if (multiplier < 0L) {
                return OptionalInt.empty();
            }
            total += amount * multiplier;
            if (total > Integer.MAX_VALUE) {
                return OptionalInt.empty();
            }
        }
        return positiveSeconds(total);
    }

    private static OptionalInt positiveSeconds(long value) {
        return value > 0 && value <= Integer.MAX_VALUE ? OptionalInt.of((int) value) : OptionalInt.empty();
    }

    private static OptionalDouble parseRange(String input) {
        try {
            double value = Double.parseDouble(input);
            if (Double.isFinite(value) && (value == -1.0D || value >= 0.0D)) {
                return OptionalDouble.of(value);
            }
        } catch (NumberFormatException ignored) {
        }
        return OptionalDouble.empty();
    }

    private static Optional<Center> parseCenter(String input) {
        String[] parts = input.split(":", -1);
        if (parts.length != 4 || parts[0].isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Center(parts[0], Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum Action {
        JOIN,
        LEAVE,
        START
    }

    record Center(String worldName, double x, double y, double z) {
    }

    record Request(Action action, OptionalInt seconds, OptionalDouble range, Optional<Center> center,
                   Optional<String> message, boolean force) {
    }
}
