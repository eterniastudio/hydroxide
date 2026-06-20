package net.axther.hydroxide.modules.mail;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailDurationParserTest {

    @Test
    void parsesCmiStyleCompoundDurations() {
        assertEquals(Duration.ofHours(24), MailDurationParser.parse("24h").orElseThrow());
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(30), MailDurationParser.parse("1d2h30m").orElseThrow());
        assertEquals(Duration.ofDays(30), MailDurationParser.parse("1M").orElseThrow());
        assertEquals(Duration.ofDays(365), MailDurationParser.parse("1y").orElseThrow());
    }

    @Test
    void rejectsBlankZeroOrMalformedDurations() {
        assertTrue(MailDurationParser.parse("").isEmpty());
        assertTrue(MailDurationParser.parse("0s").isEmpty());
        assertTrue(MailDurationParser.parse("forever").isEmpty());
        assertTrue(MailDurationParser.parse("10q").isEmpty());
    }
}
