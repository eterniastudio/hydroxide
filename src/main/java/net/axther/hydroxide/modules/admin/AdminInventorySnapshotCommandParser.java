package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AdminInventorySnapshotCommandParser {

    private AdminInventorySnapshotCommandParser() {
    }

    static Optional<SaveRequest> parseSave(List<String> args) {
        if (args.isEmpty() || args.size() > 3) {
            return Optional.empty();
        }
        String playerName = args.getFirst().trim();
        if (playerName.isBlank()) {
            return Optional.empty();
        }

        Optional<String> id = Optional.empty();
        boolean silent = false;
        for (String raw : args.subList(1, args.size())) {
            String token = raw.trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            if (token.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (id.isEmpty()) {
                id = Optional.of(normalizeId(token));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new SaveRequest(playerName, id, silent));
    }

    static Optional<CheckRequest> parseCheck(List<String> args) {
        if (args.isEmpty() || args.size() > 3) {
            return Optional.empty();
        }
        String playerName = args.getFirst().trim();
        if (playerName.isBlank()) {
            return Optional.empty();
        }

        Optional<String> id = Optional.empty();
        boolean edit = false;
        for (String raw : args.subList(1, args.size())) {
            String token = raw.trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            if (token.equalsIgnoreCase("-e")) {
                edit = true;
            } else if (id.isEmpty()) {
                id = Optional.of(normalizeId(token));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new CheckRequest(playerName, id, edit));
    }

    static Optional<ListRequest> parseList(List<String> args) {
        if (args.size() != 1 || args.getFirst().trim().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ListRequest(args.getFirst().trim()));
    }

    static Optional<LoadRequest> parseLoad(List<String> args) {
        if (args.size() < 2 || args.size() > 3
                || args.getFirst().trim().isBlank()
                || args.get(1).trim().isBlank()) {
            return Optional.empty();
        }
        Optional<String> id = args.size() == 3 ? Optional.of(normalizeId(args.get(2))) : Optional.empty();
        return Optional.of(new LoadRequest(args.getFirst().trim(), args.get(1).trim(), id));
    }

    static Optional<RemoveRequest> parseRemove(List<String> args) {
        if (args.isEmpty() || args.size() > 2 || args.getFirst().trim().isBlank()) {
            return Optional.empty();
        }
        RemoveTarget selector = args.size() == 1
                ? RemoveTarget.last()
                : RemoveTarget.from(args.get(1)).orElse(null);
        if (selector == null) {
            return Optional.empty();
        }
        return Optional.of(new RemoveRequest(args.getFirst().trim(), selector));
    }

    static RemoveAllRequest parseRemoveAll(List<String> args) {
        return new RemoveAllRequest(args.size() == 1 && args.getFirst().equalsIgnoreCase("confirmed"));
    }

    static String normalizeId(String input) {
        return input.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
    }

    record SaveRequest(String playerName, Optional<String> id, boolean silent) {
    }

    record CheckRequest(String playerName, Optional<String> id, boolean edit) {
    }

    record ListRequest(String playerName) {
    }

    record LoadRequest(String sourceName, String targetName, Optional<String> id) {
    }

    record RemoveRequest(String playerName, RemoveTarget selector) {
    }

    record RemoveTarget(RemoveSelector type, Optional<String> id) {
        static RemoveTarget last() {
            return new RemoveTarget(RemoveSelector.LAST, Optional.empty());
        }

        static Optional<RemoveTarget> from(String raw) {
            String token = normalizeId(raw);
            if (token.isBlank()) {
                return Optional.empty();
            }
            return switch (token) {
                case "all" -> Optional.of(new RemoveTarget(RemoveSelector.ALL, Optional.empty()));
                case "last" -> Optional.of(last());
                default -> Optional.of(new RemoveTarget(RemoveSelector.ID, Optional.of(token)));
            };
        }
    }

    enum RemoveSelector {
        ID,
        LAST,
        ALL
    }

    record RemoveAllRequest(boolean confirmed) {
    }
}
