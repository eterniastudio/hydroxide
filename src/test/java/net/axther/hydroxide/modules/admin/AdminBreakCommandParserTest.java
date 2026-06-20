package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminBreakCommandParserTest {

    @Test
    void parsesSelfTargetAndSilentFlag() {
        AdminBreakCommandParser.Request self = AdminBreakCommandParser.parse(List.of()).orElseThrow();
        AdminBreakCommandParser.Request target = AdminBreakCommandParser.parse(List.of("Alex", "-s")).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertTrue(target.silent());
    }

    @Test
    void rejectsMalformedRequests() {
        assertTrue(AdminBreakCommandParser.parse(List.of("Alex", "Steve")).isEmpty());
        assertTrue(AdminBreakCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(AdminBreakCommandParser.parse(List.of("-s", "-s")).isEmpty());
    }
}
