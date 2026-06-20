package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class AdminAccountIndex {

    private final List<Account> accounts;

    private AdminAccountIndex(List<Account> accounts) {
        this.accounts = accounts.stream()
                .sorted(Comparator.comparing(Account::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    static AdminAccountIndex from(YamlConfiguration yaml) {
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return new AdminAccountIndex(List.of());
        }
        List<Account> accounts = players.getKeys(false).stream()
                .map(uuid -> account(players, uuid))
                .filter(Objects::nonNull)
                .toList();
        return new AdminAccountIndex(accounts);
    }

    List<Account> find(String query, Function<String, String> ipHasher) {
        String normalized = query.toLowerCase(Locale.ROOT);
        String targetHash = accounts.stream()
                .filter(account -> account.name().equalsIgnoreCase(query)
                        || account.uuid().toString().equalsIgnoreCase(query))
                .map(Account::ipHash)
                .findFirst()
                .orElseGet(() -> accounts.stream()
                        .map(Account::ipHash)
                        .filter(ipHash -> ipHash.equalsIgnoreCase(query))
                        .findFirst()
                        .orElseGet(() -> directHash(query) ? query.toLowerCase(Locale.ROOT) : ipHasher.apply(query)));

        if (targetHash == null || targetHash.isBlank()) {
            return List.of();
        }

        String finalTargetHash = targetHash.toLowerCase(Locale.ROOT);
        return accounts.stream()
                .filter(account -> account.ipHash().equalsIgnoreCase(finalTargetHash))
                .filter(account -> !normalized.isBlank())
                .toList();
    }

    List<AccountGroup> duplicateGroups() {
        return accounts.stream()
                .collect(Collectors.groupingBy(account -> account.ipHash().toLowerCase(Locale.ROOT)))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> new AccountGroup(entry.getKey(), sorted(entry.getValue())))
                .sorted(Comparator.comparing(group -> group.accounts().getFirst().name(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Account> sorted(List<Account> values) {
        return values.stream()
                .sorted(Comparator.comparing(Account::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static Account account(ConfigurationSection players, String uuidValue) {
        String name = players.getString(uuidValue + ".name");
        String ipHash = players.getString(uuidValue + ".ip-hash");
        if (name == null || name.isBlank() || ipHash == null || ipHash.isBlank()) {
            return null;
        }
        try {
            return new Account(UUID.fromString(uuidValue), name, ipHash);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean directHash(String query) {
        return query.matches("(?i)[a-f0-9]{64}");
    }

    record Account(UUID uuid, String name, String ipHash) {
    }

    record AccountGroup(String ipHash, List<Account> accounts) {
    }
}
