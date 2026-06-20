package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationDurationParserTest {

    @Test
    void parsesCompoundDurations() {
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(30),
                ModerationDurationParser.parse("1d2h30m").orElseThrow());
        assertEquals(Duration.ofSeconds(45), ModerationDurationParser.parse("45s").orElseThrow());
    }

    @Test
    void rejectsInvalidDurations() {
        assertTrue(ModerationDurationParser.parse("").isEmpty());
        assertTrue(ModerationDurationParser.parse("forever").isEmpty());
        assertTrue(ModerationDurationParser.parse("0s").isEmpty());
    }
}
