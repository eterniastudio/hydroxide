package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationScaleCommandParserTest {

    @Test
    void parsesSelfAndTargetSetRequests() {
        ModerationScaleCommandParser.Request self = ModerationScaleCommandParser.parse(List.of("set", "1.5")).orElseThrow();
        ModerationScaleCommandParser.Request target = ModerationScaleCommandParser.parse(List.of("set", "Alex", "0.75")).orElseThrow();

        assertEquals(ModerationScaleCommandParser.Action.SET, self.action());
        assertTrue(self.targetName().isEmpty());
        assertEquals(1.5D, self.amount().orElseThrow());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertEquals(0.75D, target.amount().orElseThrow());
        assertFalse(target.silent());
    }

    @Test
    void parsesAddTakeClearAndSilentRequests() {
        ModerationScaleCommandParser.Request add = ModerationScaleCommandParser.parse(List.of("add", "Steve", "0.25", "-s")).orElseThrow();
        ModerationScaleCommandParser.Request take = ModerationScaleCommandParser.parse(List.of("take", "0.5")).orElseThrow();
        ModerationScaleCommandParser.Request clear = ModerationScaleCommandParser.parse(List.of("clear", "Steve", "-s")).orElseThrow();

        assertEquals(ModerationScaleCommandParser.Action.ADD, add.action());
        assertEquals("Steve", add.targetName().orElseThrow());
        assertEquals(0.25D, add.amount().orElseThrow());
        assertTrue(add.silent());
        assertEquals(ModerationScaleCommandParser.Action.TAKE, take.action());
        assertTrue(take.targetName().isEmpty());
        assertEquals(0.5D, take.amount().orElseThrow());
        assertEquals(ModerationScaleCommandParser.Action.CLEAR, clear.action());
        assertEquals("Steve", clear.targetName().orElseThrow());
        assertTrue(clear.amount().isEmpty());
        assertTrue(clear.silent());
    }

    @Test
    void rejectsInvalidAmountsAndShapes() {
        assertTrue(ModerationScaleCommandParser.parse(List.of()).isEmpty());
        assertTrue(ModerationScaleCommandParser.parse(List.of("set")).isEmpty());
        assertTrue(ModerationScaleCommandParser.parse(List.of("set", "-1")).isEmpty());
        assertTrue(ModerationScaleCommandParser.parse(List.of("set", "NaN")).isEmpty());
        assertTrue(ModerationScaleCommandParser.parse(List.of("set", "Infinity")).isEmpty());
        assertTrue(ModerationScaleCommandParser.parse(List.of("clear", "Steve", "extra")).isEmpty());
        assertTrue(ModerationScaleCommandParser.parse(List.of("multiply", "Steve", "2")).isEmpty());
    }
}
