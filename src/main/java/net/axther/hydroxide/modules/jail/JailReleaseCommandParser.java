package net.axther.hydroxide.modules.jail;

import java.util.List;
import java.util.Optional;

final class JailReleaseCommandParser {

    private JailReleaseCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        boolean silent = false;
        String targetName = null;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (targetName == null) {
                targetName = arg;
            } else {
                return Optional.empty();
            }
        }
        return targetName == null ? Optional.empty() : Optional.of(new Request(targetName, silent));
    }

    record Request(String targetName, boolean silent) {
    }
}
