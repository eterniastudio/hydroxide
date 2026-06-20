package net.axther.hydroxide.modules.teleport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class DownCommandParser {

    private DownCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean silent = false;
        boolean max = false;
        for (String arg : args) {
            String lowered = arg.toLowerCase(Locale.ROOT);
            if (lowered.equals("-s")) {
                silent = true;
            } else if (lowered.equals("max")) {
                max = true;
            } else {
                values.add(arg);
            }
        }
        if (values.size() > 1) {
            return Optional.empty();
        }
        return Optional.of(new Request(values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst()), max, silent));
    }

    record Request(Optional<String> targetName, boolean max, boolean silent) {
    }
}
