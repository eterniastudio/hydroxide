package net.axther.hydroxide.modules.motd;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomTextCommandParserTest {

    @Test
    void parsesTextNameForSelfDisplay() {
        CustomTextCommandParser.Request request = CustomTextCommandParser.parse(List.of("welcome")).orElseThrow();

        assertEquals("welcome", request.name());
        assertTrue(request.targetName().isEmpty());
        assertTrue(request.sourceName().isEmpty());
    }

    @Test
    void parsesTargetAndSourcePlayer() {
        CustomTextCommandParser.Request request = CustomTextCommandParser
                .parse(List.of("welcome", "Alex", "Steve"))
                .orElseThrow();

        assertEquals("welcome", request.name());
        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals("Steve", request.sourceName().orElseThrow());
    }

    @Test
    void acceptsAllTarget() {
        CustomTextCommandParser.Request request = CustomTextCommandParser
                .parse(List.of("rules", "all"))
                .orElseThrow();

        assertEquals("all", request.targetName().orElseThrow());
        assertTrue(request.targetsAll());
    }

    @Test
    void rejectsMissingOrExtraArguments() {
        assertTrue(CustomTextCommandParser.parse(List.of()).isEmpty());
        assertTrue(CustomTextCommandParser.parse(List.of(" ")).isEmpty());
        assertTrue(CustomTextCommandParser.parse(List.of("welcome", "Alex", "Steve", "extra")).isEmpty());
    }
}
