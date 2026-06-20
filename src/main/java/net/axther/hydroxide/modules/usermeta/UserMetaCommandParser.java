package net.axther.hydroxide.modules.usermeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class UserMetaCommandParser {

    private UserMetaCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() < 2 || args.getFirst().isBlank()) {
            return Optional.empty();
        }

        String playerName = args.getFirst();
        List<String> tokens = new ArrayList<>(args.subList(1, args.size()));
        boolean silent = tokens.removeIf(token -> token.equalsIgnoreCase("-s"));
        if (tokens.isEmpty()) {
            return Optional.empty();
        }

        Action action = switch (tokens.getFirst().toLowerCase(Locale.ROOT)) {
            case "add", "set" -> Action.ADD;
            case "remove", "delete", "del" -> Action.REMOVE;
            case "clear" -> Action.CLEAR;
            case "list", "show" -> Action.LIST;
            case "increment", "inc" -> Action.INCREMENT;
            default -> null;
        };
        if (action == null) {
            return Optional.empty();
        }

        return switch (action) {
            case ADD -> parseKeyValue(playerName, action, tokens, silent);
            case INCREMENT -> parseIncrement(playerName, tokens, silent);
            case REMOVE -> parseSingleKey(playerName, action, tokens, silent);
            case CLEAR, LIST -> tokens.size() == 1
                    ? Optional.of(new Request(action, playerName, Optional.empty(), Optional.empty(), silent))
                    : Optional.empty();
        };
    }

    private static Optional<Request> parseKeyValue(String playerName, Action action, List<String> tokens, boolean silent) {
        if (tokens.size() < 3) {
            return Optional.empty();
        }
        String key = UserMetaService.normalizeKey(tokens.get(1));
        String value = String.join(" ", tokens.subList(2, tokens.size())).trim();
        if (key.isBlank() || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(action, playerName, Optional.of(key), Optional.of(value), silent));
    }

    private static Optional<Request> parseIncrement(String playerName, List<String> tokens, boolean silent) {
        if (tokens.size() != 3) {
            return Optional.empty();
        }
        String key = UserMetaService.normalizeKey(tokens.get(1));
        String value = tokens.get(2).trim();
        if (key.isBlank() || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(Action.INCREMENT, playerName, Optional.of(key), Optional.of(value), silent));
    }

    private static Optional<Request> parseSingleKey(String playerName, Action action, List<String> tokens, boolean silent) {
        if (tokens.size() != 2) {
            return Optional.empty();
        }
        String key = UserMetaService.normalizeKey(tokens.get(1));
        if (key.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new Request(action, playerName, Optional.of(key), Optional.empty(), silent));
    }

    enum Action {
        ADD,
        REMOVE,
        CLEAR,
        LIST,
        INCREMENT
    }

    record Request(Action action, String playerName, Optional<String> key, Optional<String> value, boolean silent) {
    }
}
