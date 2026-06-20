package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class RecipeCommandParser {

    private RecipeCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.of(new Request(Source.HAND, Optional.empty()));
        }
        if (args.size() > 1) {
            return Optional.empty();
        }

        String argument = args.getFirst().toLowerCase(Locale.ROOT);
        if (argument.equals("hand") || argument.equals("held")) {
            return Optional.of(new Request(Source.HAND, Optional.empty()));
        }
        return Optional.of(new Request(Source.MATERIAL, Optional.of(argument)));
    }

    enum Source {
        HAND,
        MATERIAL
    }

    record Request(Source source, Optional<String> material) {
    }
}
