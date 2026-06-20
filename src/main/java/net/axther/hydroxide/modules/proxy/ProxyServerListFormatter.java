package net.axther.hydroxide.modules.proxy;

import java.util.List;
import java.util.Locale;

final class ProxyServerListFormatter {

    private ProxyServerListFormatter() {
    }

    static Snapshot snapshot(List<String> configuredServers, String filter) {
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        List<String> allServers = configuredServers.stream()
                .filter(server -> server != null && !server.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> visible = allServers.stream()
                .filter(server -> normalizedFilter.isBlank()
                        || server.toLowerCase(Locale.ROOT).contains(normalizedFilter))
                .toList();
        return new Snapshot(allServers.size(), visible);
    }

    record Snapshot(int totalCount, List<String> servers) {

        int shownCount() {
            return servers.size();
        }
    }
}
