package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminNukeCommandParserTest {

    @Test
    void parsesSelfTargetAndSilentFlag() {
        AdminNukeCommandParser.Request self = AdminNukeCommandParser.parse(List.of()).orElseThrow();
        AdminNukeCommandParser.Request target = AdminNukeCommandParser.parse(List.of("Alex", "-s")).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertEquals("Alex", target.targetName().orElseThrow());
        assertTrue(target.silent());
    }

    @Test
    void rejectsMalformedRequests() {
        assertTrue(AdminNukeCommandParser.parse(List.of("Alex", "Steve")).isEmpty());
        assertTrue(AdminNukeCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(AdminNukeCommandParser.parse(List.of("-s", "-s")).isEmpty());
    }
}
