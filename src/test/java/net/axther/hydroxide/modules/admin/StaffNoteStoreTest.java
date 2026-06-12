package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaffNoteStoreTest {

    @Test
    void addsListsAndClearsNotes() {
        StaffNoteStore store = new StaffNoteStore(new YamlConfiguration());
        UUID playerId = UUID.randomUUID();

        store.add(playerId, "Admin", "First note");
        assertEquals(1, store.notes(playerId).size());

        store.clear(playerId);
        assertEquals(0, store.notes(playerId).size());
    }
}
