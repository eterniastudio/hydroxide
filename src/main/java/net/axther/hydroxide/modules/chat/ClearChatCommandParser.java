package net.axther.hydroxide.modules.chat;

import java.util.List;
import java.util.Optional;

final class ClearChatCommandParser {

    private ClearChatCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        Mode mode = Mode.GLOBAL;
        boolean sawMode = false;
        boolean silent = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (arg.equalsIgnoreCase("self")) {
                if (sawMode) {
                    return Optional.empty();
                }
                mode = Mode.SELF;
                sawMode = true;
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(new Request(mode, silent));
    }

    enum Mode {
        GLOBAL,
        SELF
    }

    record Request(Mode mode, boolean silent) {
    }
}
