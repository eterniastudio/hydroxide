package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminInventorySnapshotCommandParserTest {

    @Test
    void parsesSaveWithOptionalIdAndSilentFlag() {
        AdminInventorySnapshotCommandParser.SaveRequest request = AdminInventorySnapshotCommandParser
                .parseSave(List.of("Alex", "raid", "-s"))
                .orElseThrow();

        assertEquals("Alex", request.playerName());
        assertEquals("raid", request.id().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void parsesCheckWithLastAndEditFlags() {
        AdminInventorySnapshotCommandParser.CheckRequest request = AdminInventorySnapshotCommandParser
                .parseCheck(List.of("Alex", "last", "-e"))
                .orElseThrow();

        assertEquals("Alex", request.playerName());
        assertEquals("last", request.id().orElseThrow());
        assertTrue(request.edit());
    }

    @Test
    void parsesLoadWithSourceTargetAndId() {
        AdminInventorySnapshotCommandParser.LoadRequest request = AdminInventorySnapshotCommandParser
                .parseLoad(List.of("Alex", "Steve", "raid"))
                .orElseThrow();

        assertEquals("Alex", request.sourceName());
        assertEquals("Steve", request.targetName());
        assertEquals("raid", request.id().orElseThrow());
    }

    @Test
    void parsesRemoveSelectors() {
        AdminInventorySnapshotCommandParser.RemoveRequest request = AdminInventorySnapshotCommandParser
                .parseRemove(List.of("Alex", "all"))
                .orElseThrow();

        assertEquals("Alex", request.playerName());
        assertEquals(AdminInventorySnapshotCommandParser.RemoveSelector.ALL, request.selector().type());
        assertTrue(request.selector().id().isEmpty());
    }

    @Test
    void parsesRemoveAllConfirmation() {
        assertTrue(AdminInventorySnapshotCommandParser.parseRemoveAll(List.of("confirmed")).confirmed());
        assertFalse(AdminInventorySnapshotCommandParser.parseRemoveAll(List.of()).confirmed());
    }

    @Test
    void rejectsInvalidArguments() {
        assertTrue(AdminInventorySnapshotCommandParser.parseSave(List.of()).isEmpty());
        assertTrue(AdminInventorySnapshotCommandParser.parseSave(List.of("Alex", "one", "two")).isEmpty());
        assertTrue(AdminInventorySnapshotCommandParser.parseCheck(List.of("Alex", "one", "two")).isEmpty());
        assertTrue(AdminInventorySnapshotCommandParser.parseLoad(List.of("Alex")).isEmpty());
        assertTrue(AdminInventorySnapshotCommandParser.parseLoad(List.of("Alex", "Steve", "raid", "extra")).isEmpty());
        assertTrue(AdminInventorySnapshotCommandParser.parseRemove(List.of()).isEmpty());
        assertTrue(AdminInventorySnapshotCommandParser.parseRemove(List.of("Alex", "raid", "extra")).isEmpty());
    }
}
