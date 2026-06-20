package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationMaxHealthCommandParserTest {

    @Test
    void parsesSelfAndTargetSetRequests() {
        ModerationMaxHealthCommandParser.Request self = ModerationMaxHealthCommandParser.parse(List.of("set", "30")).orElseThrow();
        ModerationMaxHealthCommandParser.Request target = ModerationMaxHealthCommandParser.parse(List.of("set", "Alex", "40")).orElseThrow();

        assertEquals(ModerationMaxHealthCommandParser.Action.SET, self.action());
        assertTrue(self.targetName().isEmpty());
        assertEquals(30.0D, self.amount().orElseThrow());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertEquals(40.0D, target.amount().orElseThrow());
    }

    @Test
    void parsesAddTakeAndClearRequests() {
        ModerationMaxHealthCommandParser.Request add = ModerationMaxHealthCommandParser.parse(List.of("add", "Steve", "4")).orElseThrow();
        ModerationMaxHealthCommandParser.Request take = ModerationMaxHealthCommandParser.parse(List.of("take", "2")).orElseThrow();
        ModerationMaxHealthCommandParser.Request clear = ModerationMaxHealthCommandParser.parse(List.of("clear", "Steve")).orElseThrow();

        assertEquals(ModerationMaxHealthCommandParser.Action.ADD, add.action());
        assertEquals("Steve", add.targetName().orElseThrow());
        assertEquals(4.0D, add.amount().orElseThrow());
        assertEquals(ModerationMaxHealthCommandParser.Action.TAKE, take.action());
        assertTrue(take.targetName().isEmpty());
        assertEquals(2.0D, take.amount().orElseThrow());
        assertEquals(ModerationMaxHealthCommandParser.Action.CLEAR, clear.action());
        assertEquals("Steve", clear.targetName().orElseThrow());
        assertTrue(clear.amount().isEmpty());
    }

    @Test
    void rejectsInvalidAmountsAndShapes() {
        assertTrue(ModerationMaxHealthCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationMaxHealthCommandParser.parse(List.of("set")).isEmpty());
        assertTrue(ModerationMaxHealthCommandParser.parse(List.of("set", "-1")).isEmpty());
        assertTrue(ModerationMaxHealthCommandParser.parse(List.of("set", "NaN")).isEmpty());
        assertTrue(ModerationMaxHealthCommandParser.parse(List.of("set", "Infinity")).isEmpty());
        assertTrue(ModerationMaxHealthCommandParser.parse(List.of("clear", "Steve", "extra")).isEmpty());
        assertTrue(ModerationMaxHealthCommandParser.parse(List.of("multiply", "Steve", "2")).isEmpty());
    }
}
