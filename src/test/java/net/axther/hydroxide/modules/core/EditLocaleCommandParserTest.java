package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditLocaleCommandParserTest {

    @Test
    void parsesSetRequestWithJoinedValue() {
        EditLocaleCommandParser.SetRequest request = EditLocaleCommandParser.parseSet(List.of(
                "set",
                "chat.broadcast.format",
                "<gold>Server",
                "<white>{message}"
        )).orElseThrow();

        assertEquals("chat.broadcast.format", request.key());
        assertEquals("<gold>Server <white>{message}", request.value());
    }

    @Test
    void rejectsMissingSetParts() {
        assertTrue(EditLocaleCommandParser.parseSet(List.of()).isEmpty());
        assertTrue(EditLocaleCommandParser.parseSet(List.of("reload")).isEmpty());
        assertTrue(EditLocaleCommandParser.parseSet(List.of("set", "chat.broadcast.format")).isEmpty());
        assertTrue(EditLocaleCommandParser.parseSet(List.of("set", "chat.broadcast.format", "")).isEmpty());
    }
}
