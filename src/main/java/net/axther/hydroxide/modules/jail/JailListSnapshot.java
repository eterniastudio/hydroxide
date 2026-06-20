package net.axther.hydroxide.modules.jail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class JailListSnapshot {

    private final List<Cell> cells;

    private JailListSnapshot(List<Cell> cells) {
        this.cells = List.copyOf(cells);
    }

    static JailListSnapshot create(List<String> configuredJails, Collection<JailSentence> sentences,
                                   Map<UUID, String> playerNames, Optional<String> jailFilter,
                                   Optional<String> cellIdFilter, Instant now) {
        List<String> jailNames = configuredJails.stream()
                .filter(jail -> matchesFilter(jail, jailFilter, cellIdFilter))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<Cell> cells = new ArrayList<>();
        for (String jailName : jailNames) {
            List<Prisoner> prisoners = sentences.stream()
                    .filter(sentence -> sentence.jailName().equalsIgnoreCase(jailName))
                    .sorted(Comparator.comparing(sentence -> playerName(sentence.playerId(), playerNames), String.CASE_INSENSITIVE_ORDER))
                    .map(sentence -> new Prisoner(
                            sentence.playerId(),
                            playerName(sentence.playerId(), playerNames),
                            sentence.remainingSeconds(now),
                            sentence.reason()
                    ))
                    .toList();
            cells.add(new Cell(jailName, prisoners.size(), prisoners));
        }
        return new JailListSnapshot(cells);
    }

    List<Cell> cells() {
        return cells;
    }

    boolean empty() {
        return cells.isEmpty();
    }

    private static boolean matchesFilter(String jailName, Optional<String> jailFilter, Optional<String> cellIdFilter) {
        if (jailFilter.isEmpty()) {
            return true;
        }
        String normalizedJail = jailFilter.get().toLowerCase(Locale.ROOT);
        String normalizedName = jailName.toLowerCase(Locale.ROOT);
        if (cellIdFilter.isPresent()) {
            return normalizedName.equals(normalizedJail + "-" + cellIdFilter.get().toLowerCase(Locale.ROOT));
        }
        return normalizedName.equals(normalizedJail) || normalizedName.startsWith(normalizedJail + "-");
    }

    private static String playerName(UUID playerId, Map<UUID, String> playerNames) {
        return Optional.ofNullable(playerNames.get(playerId))
                .filter(name -> !name.isBlank())
                .orElse(playerId.toString());
    }

    record Cell(String jailName, int prisonerCount, List<Prisoner> prisoners) {
    }

    record Prisoner(UUID playerId, String playerName, long remainingSeconds, String reason) {
    }
}
