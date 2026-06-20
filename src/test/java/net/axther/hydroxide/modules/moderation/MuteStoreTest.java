package net.axther.hydroxide.modules.moderation;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuteStoreTest {

    @Test
    void permanentMuteStaysActiveAndRoundTripsThroughYaml() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        MuteStore store = new MuteStore();

        store.mute(new MuteRecord(playerId, "Example", "Console", "Quiet please", now, null));

        YamlConfiguration yaml = new YamlConfiguration();
        store.writeTo(yaml);
        MuteStore loaded = MuteStore.from(yaml);

        assertTrue(loaded.active(playerId, now.plusSeconds(3600)).isPresent());
        assertEquals("Quiet please", loaded.active(playerId, now).orElseThrow().reason());
    }

    @Test
    void temporaryMuteExpiresAndCanBePruned() {
        UUID playerId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-15T12:00:00Z");
        MuteStore store = new MuteStore();
        store.mute(new MuteRecord(playerId, "Example", "Console", "Timed", now, now.plusSeconds(60)));

        assertTrue(store.active(playerId, now.plusSeconds(30)).isPresent());
        assertTrue(store.active(playerId, now.plusSeconds(61)).isEmpty());

        assertTrue(store.pruneExpired(now.plusSeconds(61)));
        assertFalse(store.contains(playerId));
    }

    @Test
    void unmuteRemovesStoredMute() {
        UUID playerId = UUID.randomUUID();
        MuteStore store = new MuteStore();
        store.mute(new MuteRecord(playerId, "Example", "Console", "Reason", Instant.now(), null));

        assertTrue(store.unmute(playerId));
        assertFalse(store.contains(playerId));
        assertFalse(store.unmute(playerId));
    }
}
