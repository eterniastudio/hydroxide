package net.axther.hydroxide.modules.admin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminLastOnlineIndexTest {

    @Test
    void sortsKnownLastSeenPlayersNewestFirst() {
        YamlConfiguration yaml = new YamlConfiguration();
        addPlayer(yaml, "Alice", "2026-06-16T10:15:30Z");
        addPlayer(yaml, "Blair", "2026-06-16T12:15:30Z");
        addPlayer(yaml, "Casey", "2026-06-15T12:15:30Z");

        AdminLastOnlineIndex.Page page = AdminLastOnlineIndex.page(yaml, 1, 10);

        assertEquals(3, page.total());
        assertEquals(1, page.page());
        assertEquals(1, page.totalPages());
        assertEquals("Blair", page.entries().get(0).name());
        assertEquals("Alice", page.entries().get(1).name());
        assertEquals("Casey", page.entries().get(2).name());
    }

    @Test
    void paginatesAndKeepsMissingDatesLast() {
        YamlConfiguration yaml = new YamlConfiguration();
        addPlayer(yaml, "Alice", "2026-06-16T10:15:30Z");
        addPlayer(yaml, "Blair", "2026-06-16T12:15:30Z");
        addPlayer(yaml, "Casey", "2026-06-15T12:15:30Z");
        addPlayer(yaml, "NoDate", null);

        AdminLastOnlineIndex.Page page = AdminLastOnlineIndex.page(yaml, 2, 2);

        assertEquals(4, page.total());
        assertEquals(2, page.page());
        assertEquals(2, page.totalPages());
        assertEquals("Casey", page.entries().get(0).name());
        assertEquals("NoDate", page.entries().get(1).name());
    }

    private static void addPlayer(YamlConfiguration yaml, String name, String lastSeen) {
        String path = "players." + UUID.nameUUIDFromBytes(name.getBytes());
        yaml.set(path + ".name", name);
        if (lastSeen != null) {
            yaml.set(path + ".last-seen", lastSeen);
        }
    }
}
