package net.axther.hydroxide.modules.core;

import java.util.List;
import java.util.Optional;

final class EditLocaleCommandParser {

    private EditLocaleCommandParser() {
    }

    static Optional<SetRequest> parseSet(List<String> args) {
        if (args.size() < 3 || !args.getFirst().equalsIgnoreCase("set")) {
            return Optional.empty();
        }
        String key = args.get(1).trim();
        String value = String.join(" ", args.subList(2, args.size())).trim();
        if (key.isBlank() || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new SetRequest(key, value));
    }

    record SetRequest(String key, String value) {
    }
}
