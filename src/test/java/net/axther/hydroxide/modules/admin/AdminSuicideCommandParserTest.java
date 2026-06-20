package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSuicideCommandParserTest {

    @Test
    void defaultsToSelf() {
        AdminSuicideCommandParser.Request request = AdminSuicideCommandParser.parse(List.of()).orElseThrow();

        assertTrue(request.targetName().isEmpty());
        assertFalse(request.silent());
    }

    @Test
    void parsesOptionalTargetAndSilentFlag() {
        AdminSuicideCommandParser.Request target = AdminSuicideCommandParser.parse(List.of("Alex")).orElseThrow();
        AdminSuicideCommandParser.Request silentSelf = AdminSuicideCommandParser.parse(List.of("-s")).orElseThrow();
        AdminSuicideCommandParser.Request silentTarget = AdminSuicideCommandParser.parse(List.of("Alex", "-s")).orElseThrow();

        assertEquals("Alex", target.targetName().orElseThrow());
        assertFalse(target.silent());
        assertTrue(silentSelf.targetName().isEmpty());
        assertTrue(silentSelf.silent());
        assertEquals("Alex", silentTarget.targetName().orElseThrow());
        assertTrue(silentTarget.silent());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(AdminSuicideCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(AdminSuicideCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(AdminSuicideCommandParser.parse(List.of("Alex", "-s", "extra")).isEmpty());
    }
}
