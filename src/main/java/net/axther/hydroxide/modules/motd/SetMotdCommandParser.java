package net.axther.hydroxide.modules.motd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class SetMotdCommandParser {

    private SetMotdCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }

        List<String> tokens = new ArrayList<>(args);
        boolean silent = false;
        if (tokens.getLast().equalsIgnoreCase("-s")) {
            silent = true;
            tokens.removeLast();
        }
        if (tokens.isEmpty() || (tokens.size() == 1 && tokens.getFirst().startsWith("-"))) {
            return Optional.empty();
        }

        String text = String.join(" ", tokens).replace("\\n", "\n").trim();
        if (text.isBlank()) {
            return Optional.empty();
        }

        List<String> lines = Arrays.stream(text.split("(?i)\\s+/n\\s+|\\R", -1))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        return lines.isEmpty() ? Optional.empty() : Optional.of(new Request(lines, silent));
    }

    record Request(List<String> lines, boolean silent) {
    }
}
