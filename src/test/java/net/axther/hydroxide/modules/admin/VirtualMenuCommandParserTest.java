package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualMenuCommandParserTest {

    @Test
    void defaultsToSelfWithoutSilentFlag() {
        VirtualMenuCommandParser.Request request = VirtualMenuCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertTrue(!request.silent());
    }

    @Test
    void parsesOptionalTargetAndSilentFlag() {
        VirtualMenuCommandParser.Request target = VirtualMenuCommandParser.parse(List.of("Alex")).orElseThrow();
        VirtualMenuCommandParser.Request silentTarget = VirtualMenuCommandParser.parse(List.of("Alex", "-s")).orElseThrow();
        VirtualMenuCommandParser.Request silentSelf = VirtualMenuCommandParser.parse(List.of("-s")).orElseThrow();

        assertEquals("Alex", target.targetName().orElseThrow());
        assertTrue(!target.silent());
        assertEquals("Alex", silentTarget.targetName().orElseThrow());
        assertTrue(silentTarget.silent());
        assertTrue(silentSelf.targetName().isEmpty());
        assertTrue(silentSelf.silent());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(VirtualMenuCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(VirtualMenuCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(VirtualMenuCommandParser.parse(List.of("Alex", "-s", "extra")).isEmpty());
    }
}
