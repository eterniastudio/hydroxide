package net.axther.hydroxide.modules.proxy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxySendAllCommandParserTest {

    @Test
    void parsesTargetServerName() {
        ProxySendAllCommandParser.Request request = ProxySendAllCommandParser.parse(List.of("survival")).orElseThrow();

        assertEquals("survival", request.server());
    }

    @Test
    void rejectsMissingFlagOrExtraArguments() {
        assertTrue(ProxySendAllCommandParser.parse(List.of()).isEmpty());
        assertTrue(ProxySendAllCommandParser.parse(List.of("-s")).isEmpty());
        assertTrue(ProxySendAllCommandParser.parse(List.of("survival", "extra")).isEmpty());
    }
}
