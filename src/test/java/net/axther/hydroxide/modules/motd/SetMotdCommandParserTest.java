package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetMotdCommandParserTest {

    @Test
    void parsesMotdTextIntoLines() {
        SetMotdCommandParser.Request request = SetMotdCommandParser.parse(List.of("<green>Hello", "/n", "&7Second")).orElseThrow();

        assertEquals(List.of("<green>Hello", "&7Second"), request.lines());
        assertFalse(request.silent());
    }

    @Test
    void supportsSilentFlagAtTheEnd() {
        SetMotdCommandParser.Request request = SetMotdCommandParser.parse(List.of("<#44CCFF>Hydroxide", "-s")).orElseThrow();

        assertEquals(List.of("<#44CCFF>Hydroxide"), request.lines());
        assertTrue(request.silent());
    }

    @Test
    void splitsEscapedNewlineTokensInsideText() {
        SetMotdCommandParser.Request request = SetMotdCommandParser.parse(List.of("Line", "one\\nLine", "two")).orElseThrow();

        assertEquals(List.of("Line one", "Line two"), request.lines());
    }

    @Test
    void rejectsMissingTextAndUnknownFlagOnlyInput() {
        assertTrue(SetMotdCommandParser.parse(List.of()).isEmpty());
        assertTrue(SetMotdCommandParser.parse(List.of("-s")).isEmpty());
        assertTrue(SetMotdCommandParser.parse(List.of("-x")).isEmpty());
    }
}
