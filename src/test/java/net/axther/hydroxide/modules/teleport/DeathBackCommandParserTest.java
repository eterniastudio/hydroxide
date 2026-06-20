package net.axther.hydroxide.modules.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathBackCommandParserTest {

    @Test
    void parsesSelfTargetAndOptionalPlayerTarget() {
        DeathBackCommandParser.Request self = DeathBackCommandParser.parse(List.of()).orElseThrow();
        DeathBackCommandParser.Request target = DeathBackCommandParser.parse(List.of("Alex")).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertTrue(!self.silent());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertTrue(!target.silent());
    }

    @Test
    void parsesSilentFlagBeforeOrAfterTarget() {
        DeathBackCommandParser.Request silentSelf = DeathBackCommandParser.parse(List.of("-s")).orElseThrow();
        DeathBackCommandParser.Request silentTarget = DeathBackCommandParser.parse(List.of("Alex", "-s")).orElseThrow();
        DeathBackCommandParser.Request leadingSilentTarget = DeathBackCommandParser.parse(List.of("-s", "Alex")).orElseThrow();

        assertTrue(silentSelf.silent());
        assertTrue(silentSelf.targetName().isEmpty());
        assertTrue(silentTarget.silent());
        assertEquals("Alex", silentTarget.targetName().orElseThrow());
        assertTrue(leadingSilentTarget.silent());
        assertEquals("Alex", leadingSilentTarget.targetName().orElseThrow());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(DeathBackCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(DeathBackCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(DeathBackCommandParser.parse(List.of("Alex", "-s", "extra")).isEmpty());
    }
}
