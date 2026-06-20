package net.axther.hydroxide.modules.jail;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

final class JailToggleCommandParser {

    private static final Pattern DURATION_LIKE = Pattern.compile("\\d+[smhdwMy]?");

    private JailToggleCommandParser() {
    }

    static Optional<Request> parse(List<String> args, List<String> knownJails, Duration defaultDuration) {
        if (args.size() > 1 && looksLikeDuration(args.get(1))
                && JailCommandParser.parseDurationHint(args.get(1)).isEmpty()) {
            return Optional.empty();
        }
        return JailCommandParser.parse(args, knownJails, defaultDuration)
                .map(request -> new Request(
                        request.targetName(),
                        request.jailName(),
                        request.cellId(),
                        request.duration(),
                        request.reason(),
                        request.silent()
                ));
    }

    private static boolean looksLikeDuration(String input) {
        return input != null && DURATION_LIKE.matcher(input).matches();
    }

    record Request(String targetName, Optional<String> jailName, Optional<String> cellId,
                   Duration duration, Optional<String> reason, boolean silent) {

        JailCommandParser.Request toJailRequest() {
            return new JailCommandParser.Request(targetName, jailName, cellId, duration, reason, silent);
        }
    }
}
