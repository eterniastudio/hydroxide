package net.axther.hydroxide.modules.admin;

import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminKillCommandParserTest {

    @Test
    void requiresTargetPlayer() {
        assertTrue(AdminKillCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminKillCommandParser.parse(List.of("-s")).isEmpty());
    }

    @Test
    void parsesTargetWithCmiFlagsInAnyOrder() {
        AdminKillCommandParser.Request request = AdminKillCommandParser.parse(
                List.of("Alex", "-lightning", "-force", "-s")
        ).orElseThrow();

        assertEquals("Alex", request.targetName());
        assertTrue(request.lightning());
        assertTrue(request.force());
        assertTrue(request.silent());
        assertTrue(request.damageCause().isEmpty());
    }

    @Test
    void parsesDamageCauseCaseInsensitively() {
        AdminKillCommandParser.Request request = AdminKillCommandParser.parse(List.of("Alex", "fire_tick")).orElseThrow();

        assertEquals(EntityDamageEvent.DamageCause.FIRE_TICK, request.damageCause().orElseThrow());
        assertFalse(request.lightning());
        assertFalse(request.force());
        assertFalse(request.silent());
    }

    @Test
    void rejectsUnknownFlagsInvalidCausesAndExtraArguments() {
        assertTrue(AdminKillCommandParser.parse(List.of("Alex", "-x")).isEmpty());
        assertTrue(AdminKillCommandParser.parse(List.of("Alex", "not_a_cause")).isEmpty());
        assertTrue(AdminKillCommandParser.parse(List.of("Alex", "fall", "fire")).isEmpty());
    }
}
