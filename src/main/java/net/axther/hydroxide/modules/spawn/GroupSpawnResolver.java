package net.axther.hydroxide.modules.spawn;

import net.axther.hydroxide.storage.StoredLocation;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class GroupSpawnResolver {

    private final List<Entry> entries;

    public GroupSpawnResolver(Collection<Entry> entries) {
        this.entries = entries.stream()
                .sorted(Comparator.comparingInt(Entry::priority).reversed())
                .toList();
    }

    public Optional<StoredLocation> resolve(Collection<String> groups) {
        List<String> normalizedGroups = groups.stream()
                .map(group -> group.toLowerCase(Locale.ROOT))
                .toList();
        Optional<StoredLocation> groupMatch = entries.stream()
                .filter(entry -> normalizedGroups.contains(entry.group().toLowerCase(Locale.ROOT)))
                .map(Entry::location)
                .findFirst();
        return groupMatch.or(() -> entries.stream()
                .filter(entry -> entry.group().equalsIgnoreCase("default"))
                .map(Entry::location)
                .findFirst());
    }

    public record Entry(String group, int priority, StoredLocation location) {
    }
}
