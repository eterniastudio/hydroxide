package net.axther.hydroxide.modules.options;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerOptionsTest {

    @Test
    void persistsAndReadsOptionValues() {
        UUID playerId = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        PlayerOptions options = new PlayerOptions(yaml);

        assertTrue(options.enabled(playerId, PlayerOption.PRIVATE_MESSAGES));
        options.set(playerId, PlayerOption.PRIVATE_MESSAGES, false);

        assertFalse(new PlayerOptions(yaml).enabled(playerId, PlayerOption.PRIVATE_MESSAGES));
    }
}
