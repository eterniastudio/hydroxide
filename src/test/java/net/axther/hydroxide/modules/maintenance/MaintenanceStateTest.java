package net.axther.hydroxide.modules.maintenance;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaintenanceStateTest {

    @Test
    void readsDisabledStateWhenMissing() {
        MaintenanceState state = MaintenanceState.from(new YamlConfiguration(), "Default message");

        assertFalse(state.enabled());
        assertEquals("Default message", state.message());
    }

    @Test
    void writesEnabledStateWithMessageAndAudit() {
        YamlConfiguration yaml = new YamlConfiguration();
        MaintenanceState state = new MaintenanceState(true, "Restarting", Optional.of("Console"), Optional.of(Instant.parse("2026-06-16T12:00:00Z")));

        state.writeTo(yaml);

        assertTrue(yaml.getBoolean("enabled"));
        assertEquals("Restarting", yaml.getString("message"));
        assertEquals("Console", yaml.getString("updated-by"));
        assertEquals("2026-06-16T12:00:00Z", yaml.getString("updated-at"));
    }
}
