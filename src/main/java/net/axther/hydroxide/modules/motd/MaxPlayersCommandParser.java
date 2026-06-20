package net.axther.hydroxide.modules.motd;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

final class MaxPlayersCommandParser {

    private MaxPlayersCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(OptionalInt.empty()));
        }
        if (args.size() != 1) {
            return Optional.empty();
        }
        try {
            int amount = Integer.parseInt(args.getFirst());
            return amount > 0 ? Optional.of(new Request(OptionalInt.of(amount))) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Request(OptionalInt amount) {
    }
}
