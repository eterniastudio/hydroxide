package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HatCommandParserTest {

    @Test
    void defaultsToSelf() {
        HatCommandParser.Request request = HatCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertTrue(!request.silent());
    }

    @Test
    void parsesOptionalTargetAndSilentFlag() {
        HatCommandParser.Request target = HatCommandParser.parse(List.of("Alex")).orElseThrow();
        HatCommandParser.Request silentSelf = HatCommandParser.parse(List.of("-s")).orElseThrow();
        HatCommandParser.Request silentTarget = HatCommandParser.parse(List.of("Alex", "-s")).orElseThrow();

        assertEquals("Alex", target.targetName().orElseThrow());
        assertTrue(!target.silent());
        assertTrue(silentSelf.targetName().isEmpty());
        assertTrue(silentSelf.silent());
        assertEquals("Alex", silentTarget.targetName().orElseThrow());
        assertTrue(silentTarget.silent());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(HatCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(HatCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(HatCommandParser.parse(List.of("Alex", "-s", "extra")).isEmpty());
    }
}
