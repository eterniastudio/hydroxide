package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

final class BookEditCommandParser {

    private BookEditCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }

        return switch (args.getFirst().toLowerCase(Locale.ROOT)) {
            case "title" -> valued(Action.TITLE, args, 1);
            case "author" -> valued(Action.AUTHOR, args, 1);
            case "addpage", "add-page", "add" -> valued(Action.ADD_PAGE, args, 1);
            case "page", "setpage", "set-page" -> page(args);
            case "unlock", "editable" -> args.size() == 1
                    ? Optional.of(new Request(Action.UNLOCK, OptionalInt.empty(), -1))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    private static Optional<Request> valued(Action action, List<String> args, int valueStartIndex) {
        return args.size() > valueStartIndex
                ? Optional.of(new Request(action, OptionalInt.empty(), valueStartIndex))
                : Optional.empty();
    }

    private static Optional<Request> page(List<String> args) {
        if (args.size() < 3) {
            return Optional.empty();
        }
        try {
            int page = Integer.parseInt(args.get(1));
            return page > 0
                    ? Optional.of(new Request(Action.PAGE, OptionalInt.of(page), 2))
                    : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum Action {
        TITLE,
        AUTHOR,
        ADD_PAGE,
        PAGE,
        UNLOCK
    }

    record Request(Action action, OptionalInt page, int valueStartIndex) {
    }
}
