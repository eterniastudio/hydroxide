package net.axther.hydroxide.modules.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaytimeCommandParserTest {

    @Test
    void parsesOptionalPlayerAndPage() {
        assertTrue(PlaytimeCommandParser.parsePlaytime(List.of()).orElseThrow().playerName().isEmpty());

        PlaytimeCommandParser.PlaytimeRequest request = PlaytimeCommandParser
                .parsePlaytime(List.of("Steve"))
                .orElseThrow();

        assertEquals("Steve", request.playerName().orElseThrow());

        assertEquals(1, PlaytimeCommandParser.parseTop(List.of()).orElseThrow().page());
        assertEquals(3, PlaytimeCommandParser.parseTop(List.of("3")).orElseThrow().page());
    }

    @Test
    void rejectsTooManyArgumentsAndInvalidPages() {
        assertTrue(PlaytimeCommandParser.parsePlaytime(List.of("Steve", "extra")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseTop(List.of("0")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseTop(List.of("-1")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseTop(List.of("abc")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseTop(List.of("1", "extra")).isEmpty());
    }

    @Test
    void parsesCPlaytimeRequests() {
        assertTrue(PlaytimeCommandParser.parseCPlaytime(List.of()).orElseThrow().playerName().isEmpty());
        assertEquals("Steve", PlaytimeCommandParser.parseCPlaytime(List.of("Steve")).orElseThrow().playerName().orElseThrow());
    }

    @Test
    void rejectsInvalidCPlaytimeRequests() {
        assertTrue(PlaytimeCommandParser.parseCPlaytime(List.of("")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseCPlaytime(List.of("Steve", "extra")).isEmpty());
    }

    @Test
    void parsesCmiStyleEditPlaytimeRequests() {
        PlaytimeCommandParser.EditPlaytimeRequest self = PlaytimeCommandParser
                .parseEditPlaytime(List.of("add", "30m"))
                .orElseThrow();
        PlaytimeCommandParser.EditPlaytimeRequest target = PlaytimeCommandParser
                .parseEditPlaytime(List.of("Steve", "set", "1h", "-s"))
                .orElseThrow();

        assertEquals(PlaytimeCommandParser.EditAction.ADD, self.action());
        assertTrue(self.playerName().isEmpty());
        assertEquals(1800L, self.seconds());
        assertEquals("Steve", target.playerName().orElseThrow());
        assertEquals(PlaytimeCommandParser.EditAction.SET, target.action());
        assertEquals(3600L, target.seconds());
        assertTrue(target.silent());
    }

    @Test
    void rejectsInvalidEditPlaytimeRequests() {
        assertTrue(PlaytimeCommandParser.parseEditPlaytime(List.of()).isEmpty());
        assertTrue(PlaytimeCommandParser.parseEditPlaytime(List.of("Steve", "set")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseEditPlaytime(List.of("Steve", "add", "0")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseEditPlaytime(List.of("Steve", "take", "-5")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseEditPlaytime(List.of("Steve", "noop", "1h")).isEmpty());
        assertTrue(PlaytimeCommandParser.parseEditPlaytime(List.of("Steve", "set", "1h", "extra")).isEmpty());
    }
}
