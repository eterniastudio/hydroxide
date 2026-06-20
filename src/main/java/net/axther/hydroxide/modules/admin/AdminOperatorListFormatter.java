package net.axther.hydroxide.modules.admin;

import java.util.Comparator;
import java.util.List;

final class AdminOperatorListFormatter {

    private AdminOperatorListFormatter() {
    }

    static Snapshot snapshot(List<Entry> entries) {
        List<Entry> sorted = entries.stream()
                .map(entry -> new Entry(normalizeName(entry.name()), entry.online()))
                .sorted(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new Snapshot(sorted);
    }

    private static String normalizeName(String name) {
        return name == null || name.isBlank() ? "unknown" : name;
    }

    record Entry(String name, boolean online) {

        String stateKey() {
            return online ? "admin.oplist.state.online" : "admin.oplist.state.offline";
        }
    }

    record Snapshot(List<Entry> entries) {

        int count() {
            return entries.size();
        }
    }
}
