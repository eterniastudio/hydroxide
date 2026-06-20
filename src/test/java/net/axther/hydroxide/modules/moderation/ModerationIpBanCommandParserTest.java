package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationIpBanCommandParserTest {

    @Test
    void parsesIpTargetWithoutReason() {
        ModerationIpBanCommandParser.Request request = ModerationIpBanCommandParser.parse(List.of("192.0.2.10")).orElseThrow();

        assertEquals("192.0.2.10", request.target());
        assertTrue(request.reason().isEmpty());
        assertFalse(request.silent());
    }

    @Test
    void parsesPlayerTargetWithReason() {
        ModerationIpBanCommandParser.Request request = ModerationIpBanCommandParser.parse(List.of("Steve", "Advertising")).orElseThrow();

        assertEquals("Steve", request.target());
        assertEquals("Advertising", request.reason().orElseThrow());
        assertFalse(request.silent());
    }

    @Test
    void parsesSilentFlagWithoutAddingItToReason() {
        ModerationIpBanCommandParser.Request request = ModerationIpBanCommandParser.parse(
                List.of("Steve", "-s", "VPN", "abuse")
        ).orElseThrow();

        assertTrue(request.silent());
        assertEquals("VPN abuse", request.reason().orElseThrow());
    }

    @Test
    void rejectsMissingTarget() {
        assertTrue(ModerationIpBanCommandParser.parse(List.of()).isEmpty());
    }
}
