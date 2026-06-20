package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationFeedCommandParserTest {

    @Test
    void parsesSelfWhenNoArgumentsAreGiven() {
        ModerationFeedCommandParser.Request request = ModerationFeedCommandParser.parse(List.of()).orElseThrow();

        assertFalse(request.target().all());
        assertTrue(request.target().name().isEmpty());
    }

    @Test
    void parsesSinglePlayerTarget() {
        ModerationFeedCommandParser.Request request = ModerationFeedCommandParser.parse(List.of("Steve")).orElseThrow();

        assertFalse(request.target().all());
        assertEquals("Steve", request.target().name().orElseThrow());
    }

    @Test
    void parsesAllTargetAliases() {
        assertTrue(ModerationFeedCommandParser.parse(List.of("all")).orElseThrow().target().all());
        assertTrue(ModerationFeedCommandParser.parse(List.of("*")).orElseThrow().target().all());
        assertTrue(ModerationFeedCommandParser.parse(List.of("@a")).orElseThrow().target().all());
    }

    @Test
    void parsesSilentFlagForSelfTargetAndAll() {
        ModerationFeedCommandParser.Request self = ModerationFeedCommandParser.parse(List.of("-s")).orElseThrow();
        ModerationFeedCommandParser.Request target = ModerationFeedCommandParser.parse(List.of("Steve", "-s")).orElseThrow();
        ModerationFeedCommandParser.Request all = ModerationFeedCommandParser.parse(List.of("all", "-s")).orElseThrow();

        assertTrue(self.silent());
        assertTrue(self.target().name().isEmpty());
        assertTrue(target.silent());
        assertEquals("Steve", target.target().name().orElseThrow());
        assertTrue(all.silent());
        assertTrue(all.target().all());
    }

    @Test
    void rejectsExtraArguments() {
        assertTrue(ModerationFeedCommandParser.parse(List.of("Steve", "extra")).isEmpty());
        assertTrue(ModerationFeedCommandParser.parse(List.of("-x")).isEmpty());
    }
}
