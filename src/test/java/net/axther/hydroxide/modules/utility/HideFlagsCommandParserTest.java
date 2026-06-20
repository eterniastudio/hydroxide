package net.axther.hydroxide.modules.utility;

import org.bukkit.inventory.ItemFlag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HideFlagsCommandParserTest {

    @Test
    void parsesSingleAndMultipleFlags() {
        HideFlagsCommandParser.Request single = HideFlagsCommandParser.parse(List.of("hide_enchants")).orElseThrow();
        HideFlagsCommandParser.Request multiple = HideFlagsCommandParser.parse(List.of("hide_enchants", "hide_attributes")).orElseThrow();

        assertEquals(HideFlagsCommandParser.Action.ADD, single.action());
        assertTrue(single.flags().contains(ItemFlag.HIDE_ENCHANTS));
        assertEquals(2, multiple.flags().size());
        assertTrue(multiple.flags().contains(ItemFlag.HIDE_ATTRIBUTES));
    }

    @Test
    void parsesAllAndClearActions() {
        HideFlagsCommandParser.Request all = HideFlagsCommandParser.parse(List.of("all")).orElseThrow();
        HideFlagsCommandParser.Request clear = HideFlagsCommandParser.parse(List.of("clear")).orElseThrow();

        assertEquals(HideFlagsCommandParser.Action.ADD, all.action());
        assertEquals(ItemFlag.values().length, all.flags().size());
        assertEquals(HideFlagsCommandParser.Action.CLEAR, clear.action());
        assertTrue(clear.flags().isEmpty());
    }

    @Test
    void rejectsUnknownFlagsOrExtraClearArguments() {
        assertTrue(HideFlagsCommandParser.parse(List.of()).isEmpty());
        assertTrue(HideFlagsCommandParser.parse(List.of("not_a_flag")).isEmpty());
        assertTrue(HideFlagsCommandParser.parse(List.of("clear", "hide_enchants")).isEmpty());
    }
}
