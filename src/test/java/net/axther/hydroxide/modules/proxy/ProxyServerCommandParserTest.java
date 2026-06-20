package net.axther.hydroxide.modules.proxy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyServerCommandParserTest {

    @Test
    void parsesSelfServerHop() {
        ProxyServerCommandParser.Request request = ProxyServerCommandParser.parse(List.of("lobby")).orElseThrow();

        assertEquals("lobby", request.server());
        assertTrue(request.targetName().isEmpty());
        assertFalse(request.force());
    }

    @Test
    void parsesTargetedServerHopAndForceFlag() {
        ProxyServerCommandParser.Request target = ProxyServerCommandParser.parse(List.of("survival", "Alex")).orElseThrow();
        ProxyServerCommandParser.Request forcedTarget = ProxyServerCommandParser.parse(List.of("survival", "Alex", "-f")).orElseThrow();
        ProxyServerCommandParser.Request forcedSelf = ProxyServerCommandParser.parse(List.of("survival", "-f")).orElseThrow();

        assertEquals("Alex", target.targetName().orElseThrow());
        assertFalse(target.force());
        assertEquals("Alex", forcedTarget.targetName().orElseThrow());
        assertTrue(forcedTarget.force());
        assertTrue(forcedSelf.targetName().isEmpty());
        assertTrue(forcedSelf.force());
    }

    @Test
    void rejectsMissingServerUnknownFlagsAndExtraArguments() {
        assertTrue(ProxyServerCommandParser.parse(List.of()).isEmpty());
        assertTrue(ProxyServerCommandParser.parse(List.of("-f")).isEmpty());
        assertTrue(ProxyServerCommandParser.parse(List.of("survival", "-x")).isEmpty());
        assertTrue(ProxyServerCommandParser.parse(List.of("survival", "Alex", "-f", "extra")).isEmpty());
    }
}
