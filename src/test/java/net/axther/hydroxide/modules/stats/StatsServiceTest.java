package net.axther.hydroxide.modules.stats;

import net.axther.hydroxide.storage.YamlStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void findsRememberedPlayerByNameIgnoringCase() {
        StatsService service = new StatsService(new YamlStore(new File(tempDir.toFile(), "stats.yml")));
        UUID playerId = UUID.randomUUID();

        service.rememberName(playerId, "Steve");

        assertEquals(playerId, service.playerId("steve").orElseThrow());
    }

    @Test
    void setsStatsWithoutAccumulatingOldValue() {
        StatsService service = new StatsService(new YamlStore(new File(tempDir.toFile(), "stats.yml")));
        UUID playerId = UUID.randomUUID();

        service.increment(playerId, "playtime_seconds", 120L);
        service.set(playerId, "playtime_seconds", 45L);

        assertEquals(45L, service.value(playerId, "playtime_seconds"));
    }
}
