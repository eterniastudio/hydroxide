package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminShakeItOffCommandParserTest {

    @Test
    void defaultsToSelfWithoutSilentFlag() {
        AdminShakeItOffCommandParser.Request request = AdminShakeItOffCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertFalse(request.silent());
    }

    @Test
    void parsesOptionalTargetAndSilentFlag() {
        AdminShakeItOffCommandParser.Request target = AdminShakeItOffCommandParser.parse(List.of("Alex")).orElseThrow();
        AdminShakeItOffCommandParser.Request silentSelf = AdminShakeItOffCommandParser.parse(List.of("-s")).orElseThrow();
        AdminShakeItOffCommandParser.Request silentTarget = AdminShakeItOffCommandParser.parse(List.of("Alex", "-s")).orElseThrow();

        assertEquals("Alex", target.targetName().orElseThrow());
        assertFalse(target.silent());
        assertTrue(silentSelf.targetName().isEmpty());
        assertTrue(silentSelf.silent());
        assertEquals("Alex", silentTarget.targetName().orElseThrow());
        assertTrue(silentTarget.silent());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(AdminShakeItOffCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(AdminShakeItOffCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(AdminShakeItOffCommandParser.parse(List.of("Alex", "-s", "extra")).isEmpty());
    }
}
