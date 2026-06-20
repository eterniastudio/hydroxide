package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationBanListCommandParserTest {

    @Test
    void defaultsToFirstPageWithoutArguments() {
        ModerationBanListCommandParser.Request request = ModerationBanListCommandParser.parse(List.of()).orElseThrow();

        assertEquals(1, request.page());
    }

    @Test
    void parsesExplicitPage() {
        ModerationBanListCommandParser.Request request = ModerationBanListCommandParser.parse(List.of("3")).orElseThrow();

        assertEquals(3, request.page());
    }

    @Test
    void rejectsInvalidPageArguments() {
        assertTrue(ModerationBanListCommandParser.parse(List.of("0")).isEmpty());
        assertTrue(ModerationBanListCommandParser.parse(List.of("-1")).isEmpty());
        assertTrue(ModerationBanListCommandParser.parse(List.of("many")).isEmpty());
        assertTrue(ModerationBanListCommandParser.parse(List.of("1", "extra")).isEmpty());
    }
}
