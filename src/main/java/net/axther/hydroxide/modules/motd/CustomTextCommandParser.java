package net.axther.hydroxide.modules.motd;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class CustomTextCommandParser {

    private CustomTextCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.size() > 3 || args.stream().anyMatch(String::isBlank)) {
            return Optional.empty();
        }
        return Optional.of(new Request(
                args.getFirst().toLowerCase(Locale.ROOT),
                args.size() >= 2 ? Optional.of(args.get(1)) : Optional.empty(),
                args.size() >= 3 ? Optional.of(args.get(2)) : Optional.empty()
        ));
    }

    record Request(String name, Optional<String> targetName, Optional<String> sourceName) {

        boolean targetsAll() {
            return targetName.filter(target -> target.equalsIgnoreCase("all")).isPresent();
        }
    }
}
