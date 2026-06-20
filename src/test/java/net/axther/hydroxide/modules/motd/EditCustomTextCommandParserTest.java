package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditCustomTextCommandParserTest {

    @Test
    void defaultsToListWhenNoArgumentsAreProvided() {
        EditCustomTextCommandParser.Request request = EditCustomTextCommandParser.parse(List.of()).orElseThrow();

        assertEquals(EditCustomTextCommandParser.Action.LIST, request.action());
        assertTrue(request.name().isEmpty());
    }

    @Test
    void parsesSetWithPipeSeparatedLines() {
        EditCustomTextCommandParser.Request request = EditCustomTextCommandParser.parse(List.of(
                "set",
                "discord",
                "<#5865F2>Discord",
                "|",
                "<gray>Join example.gg"
        )).orElseThrow();

        assertEquals(EditCustomTextCommandParser.Action.SET, request.action());
        assertEquals("discord", request.name().orElseThrow());
        assertEquals(List.of("<#5865F2>Discord", "<gray>Join example.gg"), request.lines());
    }

    @Test
    void parsesOneNameActions() {
        assertEquals(EditCustomTextCommandParser.Action.SHOW,
                EditCustomTextCommandParser.parse(List.of("show", "welcome")).orElseThrow().action());
        assertEquals(EditCustomTextCommandParser.Action.DELETE,
                EditCustomTextCommandParser.parse(List.of("delete", "welcome")).orElseThrow().action());
        assertEquals(EditCustomTextCommandParser.Action.ENABLE,
                EditCustomTextCommandParser.parse(List.of("enable", "welcome")).orElseThrow().action());
        assertEquals(EditCustomTextCommandParser.Action.DISABLE,
                EditCustomTextCommandParser.parse(List.of("disable", "welcome")).orElseThrow().action());
    }

    @Test
    void rejectsIncompleteRequests() {
        assertTrue(EditCustomTextCommandParser.parse(List.of("wat")).isEmpty());
        assertTrue(EditCustomTextCommandParser.parse(List.of("show")).isEmpty());
        assertTrue(EditCustomTextCommandParser.parse(List.of("delete")).isEmpty());
        assertTrue(EditCustomTextCommandParser.parse(List.of("set", "welcome")).isEmpty());
        assertTrue(EditCustomTextCommandParser.parse(List.of("reload", "extra")).isEmpty());
    }
}
