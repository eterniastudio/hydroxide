package net.axther.hydroxide.modules.shop;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class WorthListCommandParser {

    private WorthListCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        int page = 1;
        boolean missing = false;
        String target = "";
        boolean pageSeen = false;
        boolean targetSeen = false;
        for (String raw : args) {
            if (raw.isBlank()) {
                return Optional.empty();
            }
            String token = raw.toLowerCase(Locale.ROOT);
            if (token.equals("-missing") || token.equals("missing")) {
                if (missing) {
                    return Optional.empty();
                }
                missing = true;
                continue;
            }
            if (token.startsWith("-p:") || token.startsWith("page:")) {
                if (pageSeen) {
                    return Optional.empty();
                }
                int offset = token.startsWith("-p:") ? "-p:".length() : "page:".length();
                Optional<Integer> parsed = page(token.substring(offset));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                page = parsed.orElseThrow();
                pageSeen = true;
                continue;
            }
            Optional<Integer> parsedPage = page(token);
            if (parsedPage.isPresent()) {
                if (pageSeen) {
                    return Optional.empty();
                }
                page = parsedPage.orElseThrow();
                pageSeen = true;
                continue;
            }
            if (targetSeen) {
                return Optional.empty();
            }
            target = raw;
            targetSeen = true;
        }
        return Optional.of(new Request(Optional.ofNullable(target.isBlank() ? null : target), missing, page));
    }

    private static Optional<Integer> page(String input) {
        try {
            int parsed = Integer.parseInt(input);
            return parsed > 0 ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Request(Optional<String> target, boolean missing, int page) {
    }
}
