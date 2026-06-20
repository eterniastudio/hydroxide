package net.axther.hydroxide.modules.maintenance;

import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.Optional;

record MaintenanceState(boolean enabled, String message, Optional<String> updatedBy, Optional<Instant> updatedAt) {

    static MaintenanceState from(YamlConfiguration yaml, String defaultMessage) {
        return new MaintenanceState(
                yaml.getBoolean("enabled", false),
                yaml.getString("message", defaultMessage),
                Optional.ofNullable(yaml.getString("updated-by")),
                parseInstant(yaml.getString("updated-at"))
        );
    }

    MaintenanceState withEnabled(boolean enabled, String message, String updatedBy, Instant updatedAt) {
        return new MaintenanceState(enabled, message, Optional.of(updatedBy), Optional.of(updatedAt));
    }

    void writeTo(YamlConfiguration yaml) {
        yaml.set("enabled", enabled);
        yaml.set("message", message);
        yaml.set("updated-by", updatedBy.orElse(null));
        yaml.set("updated-at", updatedAt.map(Instant::toString).orElse(null));
    }

    private static Optional<Instant> parseInstant(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(input));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
