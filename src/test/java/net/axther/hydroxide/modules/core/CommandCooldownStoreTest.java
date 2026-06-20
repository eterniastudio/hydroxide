package net.axther.hydroxide.modules.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandCooldownStoreTest {

    @Test
    void writesAndReadsActiveCooldownEntries() {
        UUID playerId = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        List<CommandCooldownTracker.Entry> entries = List.of(
                new CommandCooldownTracker.Entry(playerId, "spawn", 12_000L)
        );

        CommandCooldownStore.write(yaml, entries);

        assertEquals(entries, CommandCooldownStore.read(yaml, 5_000L));
    }

    @Test
    void skipsExpiredOrInvalidEntries() {
        UUID playerId = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("entries.0.player", playerId.toString());
        yaml.set("entries.0.command", "spawn");
        yaml.set("entries.0.ready-at", 2_000L);
        yaml.set("entries.1.player", "not-a-uuid");
        yaml.set("entries.1.command", "home");
        yaml.set("entries.1.ready-at", 12_000L);
        yaml.set("entries.2.player", playerId.toString());
        yaml.set("entries.2.command", "warp");
        yaml.set("entries.2.ready-at", 12_000L);

        assertEquals(List.of(new CommandCooldownTracker.Entry(playerId, "warp", 12_000L)),
                CommandCooldownStore.read(yaml, 5_000L));
    }
}
