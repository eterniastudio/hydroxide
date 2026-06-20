package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAntiochCommandParserTest {

    @Test
    void parsesSelfTargetAndSilentFlag() {
        AdminAntiochCommandParser.Request self = AdminAntiochCommandParser.parse(List.of()).orElseThrow();
        AdminAntiochCommandParser.Request target = AdminAntiochCommandParser.parse(List.of("Alex", "-s")).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertTrue(target.silent());
    }

    @Test
    void rejectsMalformedRequests() {
        assertTrue(AdminAntiochCommandParser.parse(List.of("Alex", "Steve")).isEmpty());
        assertTrue(AdminAntiochCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(AdminAntiochCommandParser.parse(List.of("-s", "-s")).isEmpty());
    }
}
