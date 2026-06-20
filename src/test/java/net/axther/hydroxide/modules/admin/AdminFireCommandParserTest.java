package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminFireCommandParserTest {

    @Test
    void parsesBurnWithDurationAndSilentFlag() {
        AdminFireCommandParser.BurnRequest request = AdminFireCommandParser.parseBurn(List.of(
                "Alex", "1m", "-s"
        )).orElseThrow();

        assertEquals("Alex", request.targetName());
        assertEquals(60, request.seconds());
        assertTrue(request.silent());
    }

    @Test
    void parsesBurnWithPlainSecondsAndDefaultDuration() {
        AdminFireCommandParser.BurnRequest seconds = AdminFireCommandParser.parseBurn(List.of(
                "Alex", "12"
        )).orElseThrow();
        AdminFireCommandParser.BurnRequest defaulted = AdminFireCommandParser.parseBurn(List.of(
                "Alex", "-s"
        )).orElseThrow();

        assertEquals(12, seconds.seconds());
        assertEquals(10, defaulted.seconds());
        assertTrue(defaulted.silent());
    }

    @Test
    void rejectsInvalidBurnArguments() {
        assertTrue(AdminFireCommandParser.parseBurn(List.of()).isEmpty());
        assertTrue(AdminFireCommandParser.parseBurn(List.of("Alex", "0")).isEmpty());
        assertTrue(AdminFireCommandParser.parseBurn(List.of("Alex", "-1")).isEmpty());
        assertTrue(AdminFireCommandParser.parseBurn(List.of("Alex", "forever")).isEmpty());
        assertTrue(AdminFireCommandParser.parseBurn(List.of("Alex", "10", "Steve")).isEmpty());
    }

    @Test
    void parsesExtinguishSelfTargetAndSilentFlag() {
        AdminFireCommandParser.ExtinguishRequest self = AdminFireCommandParser.parseExtinguish(List.of()).orElseThrow();
        AdminFireCommandParser.ExtinguishRequest target = AdminFireCommandParser.parseExtinguish(List.of(
                "Alex", "-s"
        )).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertTrue(target.silent());
    }

    @Test
    void rejectsInvalidExtinguishArguments() {
        assertTrue(AdminFireCommandParser.parseExtinguish(List.of("Alex", "Steve")).isEmpty());
        assertTrue(AdminFireCommandParser.parseExtinguish(List.of("-unknown")).isEmpty());
    }

    @Test
    void parsesFireballDefaultsTypeTargetAndSilentFlag() {
        AdminFireCommandParser.FireballRequest defaults = AdminFireCommandParser.parseFireball(List.of()).orElseThrow();
        AdminFireCommandParser.FireballRequest targeted = AdminFireCommandParser.parseFireball(List.of(
                "dragon", "Alex", "-s"
        )).orElseThrow();

        assertEquals(AdminFireCommandParser.FireballType.SMALL, defaults.type());
        assertTrue(defaults.targetName().isEmpty());
        assertEquals(AdminFireCommandParser.FireballType.DRAGON, targeted.type());
        assertEquals("Alex", targeted.targetName().orElseThrow());
        assertTrue(targeted.silent());
    }

    @Test
    void parsesFireballTargetWithoutType() {
        AdminFireCommandParser.FireballRequest request = AdminFireCommandParser.parseFireball(List.of("Alex")).orElseThrow();

        assertEquals(AdminFireCommandParser.FireballType.SMALL, request.type());
        assertEquals("Alex", request.targetName().orElseThrow());
    }

    @Test
    void rejectsInvalidFireballArguments() {
        assertTrue(AdminFireCommandParser.parseFireball(List.of("huge", "Alex")).isEmpty());
        assertTrue(AdminFireCommandParser.parseFireball(List.of("small", "Alex", "Steve")).isEmpty());
        assertTrue(AdminFireCommandParser.parseFireball(List.of("-x")).isEmpty());
    }
}
