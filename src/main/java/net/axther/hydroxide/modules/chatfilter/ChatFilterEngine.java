package net.axther.hydroxide.modules.chatfilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ChatFilterEngine {

    private final Policy policy;
    private final List<Pattern> blockedPatterns;
    private final Map<UUID, Long> lastMessageAt = new HashMap<>();
    private final Map<UUID, Integer> strikes = new HashMap<>();

    public ChatFilterEngine(Policy policy) {
        this.policy = policy;
        this.blockedPatterns = policy.blockedPatterns().stream()
                .filter(pattern -> !pattern.isBlank())
                .map(pattern -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE))
                .toList();
    }

    public Result moderate(UUID playerId, String message, long nowMillis) {
        String output = message == null ? "" : message;
        List<String> rules = new ArrayList<>();
        boolean blocked = false;

        for (Pattern pattern : blockedPatterns) {
            if (!pattern.matcher(output).find()) {
                continue;
            }
            rules.add("filter");
            if (policy.filterMode() == FilterMode.BLOCK) {
                blocked = true;
                break;
            }
            output = pattern.matcher(output).replaceAll(replacement());
        }

        Long last = lastMessageAt.put(playerId, nowMillis);
        if (last != null && nowMillis - last < policy.rateLimitMillis()) {
            rules.add("spam");
        }

        if (capsRatio(output) > policy.maxCapsRatio()) {
            output = output.toLowerCase(Locale.ROOT);
            rules.add("caps");
        }

        int strikeCount = strikes.getOrDefault(playerId, 0);
        if (!rules.isEmpty()) {
            strikeCount++;
            strikes.put(playerId, strikeCount);
        }
        return new Result(output, !rules.isEmpty(), blocked, List.copyOf(rules), strikeCount);
    }

    public int strikes(UUID playerId) {
        return strikes.getOrDefault(playerId, 0);
    }

    public void clear(UUID playerId) {
        lastMessageAt.remove(playerId);
        strikes.remove(playerId);
    }

    private String replacement() {
        if (policy.filterMode() == FilterMode.RANDOM_WORD && !policy.replacements().isEmpty()) {
            return policy.replacements().get(0);
        }
        return "***";
    }

    private double capsRatio(String message) {
        int letters = 0;
        int uppercase = 0;
        for (int index = 0; index < message.length(); index++) {
            char current = message.charAt(index);
            if (!Character.isLetter(current)) {
                continue;
            }
            letters++;
            if (Character.isUpperCase(current)) {
                uppercase++;
            }
        }
        return letters == 0 ? 0.0D : (double) uppercase / letters;
    }

    public enum FilterMode {
        BLOCK,
        REPLACE_ASTERISKS,
        RANDOM_WORD
    }

    public record Policy(
            List<String> blockedPatterns,
            FilterMode filterMode,
            List<String> replacements,
            double maxCapsRatio,
            long rateLimitMillis
    ) {
        public static Policy defaults() {
            return new Policy(List.of(), FilterMode.REPLACE_ASTERISKS, List.of("cookie"), 0.7D, 1000L);
        }
    }

    public record Result(String message, boolean flagged, boolean blocked, List<String> rules, int strikes) {
    }
}
