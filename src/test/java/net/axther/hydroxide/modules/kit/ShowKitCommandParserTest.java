package net.axther.hydroxide.modules.kit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowKitCommandParserTest {

    @Test
    void parsesSelfPreviewRequest() {
        ShowKitCommandParser.Request request = ShowKitCommandParser.parse(List.of("Starter")).orElseThrow();

        assertEquals("starter", request.kit());
        assertTrue(request.target().isEmpty());
    }

    @Test
    void parsesTargetedPreviewRequest() {
        ShowKitCommandParser.Request request = ShowKitCommandParser.parse(List.of("starter", "Alex")).orElseThrow();

        assertEquals("starter", request.kit());
        assertEquals("Alex", request.target().orElseThrow());
    }

    @Test
    void rejectsMissingOrExtraArguments() {
        assertTrue(ShowKitCommandParser.parse(List.of()).isEmpty());
        assertTrue(ShowKitCommandParser.parse(List.of("-preview")).isEmpty());
        assertTrue(ShowKitCommandParser.parse(List.of("starter", "Alex", "extra")).isEmpty());
    }
}
