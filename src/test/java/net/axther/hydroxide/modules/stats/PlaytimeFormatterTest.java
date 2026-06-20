package net.axther.hydroxide.modules.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaytimeFormatterTest {

    @Test
    void formatsSecondsIntoCompactDaysHoursMinutesSeconds() {
        assertEquals("0s", PlaytimeFormatter.format(0L));
        assertEquals("59s", PlaytimeFormatter.format(59L));
        assertEquals("1m 5s", PlaytimeFormatter.format(65L));
        assertEquals("2h 1m 3s", PlaytimeFormatter.format(7_263L));
        assertEquals("1d 2h 3m 4s", PlaytimeFormatter.format(93_784L));
    }

    @Test
    void clampsNegativeSecondsToZero() {
        assertEquals("0s", PlaytimeFormatter.format(-5L));
    }
}
