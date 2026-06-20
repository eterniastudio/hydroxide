package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void removesOneBasedNoteIndexAndKeepsRemainingNotes() {
        StaffNoteStore store = new StaffNoteStore(new YamlConfiguration());
        UUID playerId = UUID.randomUUID();

        store.add(playerId, "Admin", "First note");
        store.add(playerId, "Admin", "Second note");

        assertTrue(store.remove(playerId, 1).orElseThrow().contains("First note"));
        assertEquals(1, store.notes(playerId).size());
        assertTrue(store.notes(playerId).getFirst().contains("Second note"));
    }

    @Test
    void removeReturnsEmptyForInvalidOneBasedIndex() {
        StaffNoteStore store = new StaffNoteStore(new YamlConfiguration());
        UUID playerId = UUID.randomUUID();

        store.add(playerId, "Admin", "Only note");

        assertTrue(store.remove(playerId, 0).isEmpty());
        assertTrue(store.remove(playerId, 2).isEmpty());
        assertEquals(1, store.notes(playerId).size());
    }
}
