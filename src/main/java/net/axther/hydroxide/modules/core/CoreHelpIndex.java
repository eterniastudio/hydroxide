package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

final class CoreHelpIndex {

    private final List<Entry> entries;

    private CoreHelpIndex(List<Entry> entries) {
        this.entries = entries.stream()
                .sorted(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static CoreHelpIndex from(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection("commands");
        if (section == null) {
            return new CoreHelpIndex(List.of());
        }
        return new CoreHelpIndex(section.getKeys(false).stream()
                .map(command -> new Entry(
                        command,
                        section.getStringList(command + ".aliases"),
                        section.getString(command + ".description", ""),
                        section.getString(command + ".usage", "/" + command),
                        section.getString(command + ".permission", "")
                ))
                .toList());
    }

    List<Entry> find(String keyword, Predicate<String> canUsePermission) {
        String lowered = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        return entries.stream()
                .filter(entry -> entry.permission().isBlank() || canUsePermission.test(entry.permission()))
                .filter(entry -> lowered.isBlank() || entry.matches(lowered))
                .toList();
    }

    static Page page(List<Entry> entries, int requestedPage, int pageSize) {
        int safePageSize = Math.max(1, pageSize);
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) safePageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int from = Math.min(entries.size(), (page - 1) * safePageSize);
        int to = Math.min(entries.size(), from + safePageSize);
        return new Page(page, totalPages, entries.subList(from, to));
    }

    record Entry(String name, List<String> aliases, String description, String usage, String permission) {

        private boolean matches(String keyword) {
            return name.toLowerCase(Locale.ROOT).contains(keyword)
                    || aliases.stream().anyMatch(alias -> alias.toLowerCase(Locale.ROOT).contains(keyword))
                    || description.toLowerCase(Locale.ROOT).contains(keyword)
                    || usage.toLowerCase(Locale.ROOT).contains(keyword)
                    || permission.toLowerCase(Locale.ROOT).contains(keyword);
        }
    }

    record Page(int page, int totalPages, List<Entry> entries) {
    }
}
