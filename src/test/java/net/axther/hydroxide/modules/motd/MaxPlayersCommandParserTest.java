package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxPlayersCommandParserTest {

    @Test
    void emptyArgumentsShowCurrentLimit() {
        MaxPlayersCommandParser.Request request = MaxPlayersCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.amount().isEmpty());
    }

    @Test
    void parsesPositivePlayerLimit() {
        MaxPlayersCommandParser.Request request = MaxPlayersCommandParser.parse(List.of("250")).orElseThrow();

        assertEquals(250, request.amount().orElseThrow());
    }

    @Test
    void rejectsInvalidAmountsAndExtraArguments() {
        assertTrue(MaxPlayersCommandParser.parse(List.of("0")).isEmpty());
        assertTrue(MaxPlayersCommandParser.parse(List.of("-1")).isEmpty());
        assertTrue(MaxPlayersCommandParser.parse(List.of("abc")).isEmpty());
        assertTrue(MaxPlayersCommandParser.parse(List.of("25", "extra")).isEmpty());
    }
}
