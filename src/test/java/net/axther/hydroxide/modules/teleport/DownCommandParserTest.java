package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownCommandParserTest {

    @Test
    void parsesSelfTargetAndMaxFlag() {
        DownCommandParser.Request self = DownCommandParser.parse(List.of()).orElseThrow();
        DownCommandParser.Request target = DownCommandParser.parse(List.of("Alex")).orElseThrow();
        DownCommandParser.Request max = DownCommandParser.parse(List.of("max")).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertFalse(self.max());
        assertTrue(max.max());
    }

    @Test
    void parsesSilentAndMaxInAnyOrder() {
        DownCommandParser.Request first = DownCommandParser.parse(List.of("Alex", "-s", "max")).orElseThrow();
        DownCommandParser.Request second = DownCommandParser.parse(List.of("max", "-s", "Alex")).orElseThrow();

        assertEquals("Alex", first.targetName().orElseThrow());
        assertEquals("Alex", second.targetName().orElseThrow());
        assertTrue(first.max());
        assertTrue(second.max());
        assertTrue(first.silent());
        assertTrue(second.silent());
    }

    @Test
    void rejectsMoreThanOneTarget() {
        assertTrue(DownCommandParser.parse(List.of("Alex", "Steve")).isEmpty());
    }
}
