package net.axther.hydroxide.modules.stats;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class PlaytimeCommandParser {

    private PlaytimeCommandParser() {
    }

    static Optional<PlaytimeRequest> parsePlaytime(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new PlaytimeRequest(Optional.empty()));
        }
        if (args.size() != 1 || args.getFirst().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new PlaytimeRequest(Optional.of(args.getFirst().trim())));
    }

    static Optional<TopRequest> parseTop(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new TopRequest(1));
        }
        if (args.size() != 1) {
            return Optional.empty();
        }
        try {
            int page = Integer.parseInt(args.getFirst());
            return page > 0 ? Optional.of(new TopRequest(page)) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    static Optional<CPlaytimeRequest> parseCPlaytime(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new CPlaytimeRequest(Optional.empty()));
        }
        if (args.size() != 1 || args.getFirst().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CPlaytimeRequest(Optional.of(args.getFirst().trim())));
    }

    static Optional<EditPlaytimeRequest> parseEditPlaytime(List<String> args) {
        List<String> values = new java.util.ArrayList<>(args);
        boolean silent = values.removeIf("-s"::equalsIgnoreCase);
        if (values.size() == 2) {
            Optional<EditAction> action = editAction(values.get(0));
            Optional<Long> seconds = durationSeconds(values.get(1));
            if (action.isEmpty() || seconds.isEmpty() || !validAmount(action.orElseThrow(), seconds.orElseThrow())) {
                return Optional.empty();
            }
            return Optional.of(new EditPlaytimeRequest(Optional.empty(), action.orElseThrow(), seconds.orElseThrow(), silent));
        }
        if (values.size() == 3) {
            Optional<EditAction> action = editAction(values.get(1));
            Optional<Long> seconds = durationSeconds(values.get(2));
            if (values.getFirst().isBlank() || action.isEmpty() || seconds.isEmpty()
                    || !validAmount(action.orElseThrow(), seconds.orElseThrow())) {
                return Optional.empty();
            }
            return Optional.of(new EditPlaytimeRequest(Optional.of(values.getFirst()), action.orElseThrow(), seconds.orElseThrow(), silent));
        }
        return Optional.empty();
    }

    private static Optional<EditAction> editAction(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "add", "give" -> Optional.of(EditAction.ADD);
            case "take", "remove" -> Optional.of(EditAction.TAKE);
            case "set" -> Optional.of(EditAction.SET);
            default -> Optional.empty();
        };
    }

    private static Optional<Long> durationSeconds(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        if (input.matches("\\d+")) {
            return Optional.of(Long.parseLong(input));
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)(\\d+)([smhdw])")
                .matcher(input);
        long seconds = 0L;
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) {
                return Optional.empty();
            }
            long amount = Long.parseLong(matcher.group(1));
            seconds += switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "s" -> amount;
                case "m" -> amount * 60L;
                case "h" -> amount * 3600L;
                case "d" -> amount * 86_400L;
                case "w" -> amount * 604_800L;
                default -> 0L;
            };
            end = matcher.end();
        }
        return end == input.length() ? Optional.of(seconds) : Optional.empty();
    }

    private static boolean validAmount(EditAction action, long seconds) {
        return action == EditAction.SET ? seconds >= 0L : seconds > 0L;
    }

    record PlaytimeRequest(Optional<String> playerName) {
    }

    record TopRequest(int page) {
    }

    record CPlaytimeRequest(Optional<String> playerName) {
    }

    enum EditAction {
        ADD,
        TAKE,
        SET
    }

    record EditPlaytimeRequest(Optional<String> playerName, EditAction action, long seconds, boolean silent) {
    }
}
