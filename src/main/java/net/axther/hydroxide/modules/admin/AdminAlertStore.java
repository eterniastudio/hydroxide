package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class AdminAlertStore {

    private static final String ROOT = "alerts";

    private final YamlConfiguration yaml;

    AdminAlertStore(YamlConfiguration yaml) {
        this.yaml = yaml;
    }

    void add(UUID playerId, String playerName, String issuer, String reason) {
        String path = path(playerId);
        yaml.set(path + ".name", playerName);
        yaml.set(path + ".issuer", issuer);
        yaml.set(path + ".reason", reason);
        yaml.set(path + ".created-at", Instant.now().toString());
    }

    Optional<Alert> find(UUID playerId) {
        ConfigurationSection section = yaml.getConfigurationSection(path(playerId));
        return section == null ? Optional.empty() : Optional.of(read(playerId, section));
    }

    List<Alert> list() {
        ConfigurationSection section = yaml.getConfigurationSection(ROOT);
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream()
                .map(this::parseUuid)
                .flatMap(Optional::stream)
                .flatMap(playerId -> find(playerId).stream())
                .sorted(Comparator.comparing(Alert::playerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    boolean remove(UUID playerId) {
        if (find(playerId).isEmpty()) {
            return false;
        }
        yaml.set(path(playerId), null);
        return true;
    }

    private Alert read(UUID playerId, ConfigurationSection section) {
        return new Alert(
                playerId,
                section.getString("name", playerId.toString()),
                section.getString("issuer", "unknown"),
                section.getString("reason", ""),
                section.getString("created-at", "unknown")
        );
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String path(UUID playerId) {
        return ROOT + "." + playerId;
    }

    record Alert(UUID playerId, String playerName, String issuer, String reason, String createdAt) {
    }
}
