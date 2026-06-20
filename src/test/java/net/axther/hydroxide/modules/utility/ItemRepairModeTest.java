package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemRepairModeTest {

    @Test
    void defaultsToHandRepair() {
        assertEquals(ItemRepairMode.HAND, ItemRepairMode.from(List.of()).orElseThrow());
        assertEquals(ItemRepairMode.HAND, ItemRepairMode.from(List.of("hand")).orElseThrow());
        assertEquals(ItemRepairMode.HAND, ItemRepairMode.from(List.of("held")).orElseThrow());
    }

    @Test
    void parsesAllRepairAliases() {
        assertEquals(ItemRepairMode.ALL, ItemRepairMode.from(List.of("all")).orElseThrow());
        assertEquals(ItemRepairMode.ALL, ItemRepairMode.from(List.of("inventory")).orElseThrow());
    }

    @Test
    void rejectsUnknownOrExtraArguments() {
        assertTrue(ItemRepairMode.from(List.of("armor")).isEmpty());
        assertTrue(ItemRepairMode.from(List.of("all", "extra")).isEmpty());
    }
}
