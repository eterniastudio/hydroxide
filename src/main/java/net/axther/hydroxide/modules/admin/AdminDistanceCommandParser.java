package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;

final class AdminDistanceCommandParser {

    private AdminDistanceCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() < 1 || args.size() > 2 || args.stream().anyMatch(String::isBlank)) {
            return Optional.empty();
        }
        return Optional.of(new Request(args.getFirst(), args.size() == 2 ? Optional.of(args.get(1)) : Optional.empty()));
    }

    record Request(String firstPlayer, Optional<String> secondPlayer) {
    }
}
