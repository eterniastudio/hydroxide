package net.axther.hydroxide.modules.usermeta;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMetaServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void exposesPlaceholderFriendlyValuesAndIntegerValues() {
        UserMetaService service = new UserMetaService(new UserMetaStore(new YamlStore(tempDir.resolve("user-meta.yml").toFile())));
        UUID playerId = UUID.randomUUID();

        service.set(playerId, "Alex", "Daily.Kills", "9");
        service.set(playerId, "Alex", "rating", "2.75");

        assertEquals("9", service.value(playerId, "daily.kills").orElseThrow());
        assertEquals("9", service.integerValue(playerId, "Daily.Kills").orElseThrow());
        assertEquals("2", service.integerValue(playerId, "rating").orElseThrow());
        assertTrue(service.value(playerId, "missing").isEmpty());
    }
}
