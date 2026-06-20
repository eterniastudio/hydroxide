package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminClearInventoryCommandParserTest {

    @Test
    void defaultsToSelfFullInventory() {
        AdminClearInventoryCommandParser.Request request = AdminClearInventoryCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.target().self());
        assertFalse(request.target().all());
        assertTrue(request.itemFilter().isEmpty());
        assertTrue(request.clearTypes().isEmpty());
        assertFalse(request.silent());
    }

    @Test
    void parsesPlayerTargetWithClearTypes() {
        AdminClearInventoryCommandParser.Request request = AdminClearInventoryCommandParser.parse(List.of(
                "Steve", "+quickbar", "+offhand"
        )).orElseThrow();

        assertEquals("Steve", request.target().name().orElseThrow());
        assertTrue(request.clearTypes().contains(AdminClearInventoryCommandParser.ClearType.QUICKBAR));
        assertTrue(request.clearTypes().contains(AdminClearInventoryCommandParser.ClearType.OFFHAND));
    }

    @Test
    void parsesAllTargetWithMaterialAmountAndSilentFlag() {
        AdminClearInventoryCommandParser.Request request = AdminClearInventoryCommandParser.parse(List.of(
                "all", "diamond:5", "-s"
        )).orElseThrow();

        assertTrue(request.target().all());
        assertEquals("diamond", request.itemFilter().orElseThrow().material());
        assertEquals(5, request.itemFilter().orElseThrow().amount().orElseThrow());
        assertTrue(request.silent());
    }

    @Test
    void parsesSelfMaterialAmountWhenOnlyTokenHasAmount() {
        AdminClearInventoryCommandParser.Request request = AdminClearInventoryCommandParser.parse(List.of("iron_ingot:12")).orElseThrow();

        assertTrue(request.target().self());
        assertEquals("iron_ingot", request.itemFilter().orElseThrow().material());
        assertEquals(12, request.itemFilter().orElseThrow().amount().orElseThrow());
    }

    @Test
    void rejectsInvalidArguments() {
        assertTrue(AdminClearInventoryCommandParser.parse(List.of("Steve", "diamond", "extra")).isEmpty());
        assertTrue(AdminClearInventoryCommandParser.parse(List.of("+unknown")).isEmpty());
        assertTrue(AdminClearInventoryCommandParser.parse(List.of("all", "diamond:0")).isEmpty());
        assertTrue(AdminClearInventoryCommandParser.parse(List.of("all", ":5")).isEmpty());
    }
}
