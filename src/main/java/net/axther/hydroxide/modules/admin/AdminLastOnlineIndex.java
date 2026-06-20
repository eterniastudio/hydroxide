package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class AdminLastOnlineIndex {

    private AdminLastOnlineIndex() {
    }

    static Page page(YamlConfiguration yaml, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        List<Entry> entries = entries(yaml);
        int total = entries.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) safePageSize));
        int from = Math.min(total, (safePage - 1) * safePageSize);
        int to = Math.min(total, from + safePageSize);
        return new Page(entries.subList(from, to), safePage, totalPages, total);
    }

    private static List<Entry> entries(YamlConfiguration yaml) {
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return List.of();
        }

        List<Entry> entries = new ArrayList<>();
        for (String id : players.getKeys(false)) {
            String path = "players." + id;
            String name = yaml.getString(path + ".name", id);
            String lastSeen = yaml.getString(path + ".last-seen", "never");
            entries.add(new Entry(name, lastSeen, parseInstant(lastSeen)));
        }
        entries.sort(Comparator
                .comparing(Entry::lastSeenInstant, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private static Instant parseInstant(String input) {
        try {
            return Instant.parse(input);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    record Entry(String name, String lastSeen, Instant lastSeenInstant) {
    }

    record Page(List<Entry> entries, int page, int totalPages, int total) {
    }
}
