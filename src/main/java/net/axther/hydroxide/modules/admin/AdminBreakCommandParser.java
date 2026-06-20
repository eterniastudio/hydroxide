package net.axther.hydroxide.modules.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class AdminBreakCommandParser {

    private AdminBreakCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>();
        boolean silent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                if (silent) {
                    return Optional.empty();
                }
                silent = true;
            } else if (!arg.startsWith("-")) {
                values.add(arg);
            } else {
                return Optional.empty();
            }
        }
        if (values.size() > 1 || (!values.isEmpty() && values.getFirst().isBlank())) {
            return Optional.empty();
        }
        return Optional.of(new Request(values.isEmpty() ? Optional.empty() : Optional.of(values.getFirst()), silent));
    }

    record Request(Optional<String> targetName, boolean silent) {
    }
}
