package net.axther.hydroxide.modules.world;

import java.util.List;
import java.util.Optional;

final class WorldGameruleCommandParser {

    private WorldGameruleCommandParser() {
    }

    static Optional<Request> parse(List<String> args, boolean senderIsPlayer) {
        if (args.size() == 2 && senderIsPlayer && valid(args.get(0)) && valid(args.get(1))) {
            return Optional.of(new Request(Optional.empty(), args.get(0), args.get(1)));
        }
        if (args.size() == 3 && valid(args.get(0)) && valid(args.get(1)) && valid(args.get(2))) {
            return Optional.of(new Request(Optional.of(args.get(0)), args.get(1), args.get(2)));
        }
        return Optional.empty();
    }

    private static boolean valid(String value) {
        return value != null && !value.isBlank();
    }

    record Request(Optional<String> worldName, String setting, String value) {
    }
}
