package net.axther.hydroxide.modules.builder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class BlockCyclingCommandParser {

    private BlockCyclingCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Direction.FORWARD));
        }
        if (args.size() > 1) {
            return Optional.empty();
        }
        return Direction.from(args.getFirst()).map(Request::new);
    }

    enum Direction {
        FORWARD,
        BACKWARD;

        static Optional<Direction> from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "forward", "forwards", "next", "+" -> Optional.of(FORWARD);
                case "backward", "backwards", "back", "previous", "prev", "-" -> Optional.of(BACKWARD);
                default -> Optional.empty();
            };
        }
    }

    record Request(Direction direction) {
    }
}
