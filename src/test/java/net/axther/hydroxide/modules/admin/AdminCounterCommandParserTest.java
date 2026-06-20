package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminCounterCommandParserTest {

    @Test
    void parsesJoinAndLeaveRequests() {
        AdminCounterCommandParser.Request join = AdminCounterCommandParser.parse(List.of("join")).orElseThrow();
        AdminCounterCommandParser.Request leave = AdminCounterCommandParser.parse(List.of("leave")).orElseThrow();

        assertEquals(AdminCounterCommandParser.Action.JOIN, join.action());
        assertEquals(AdminCounterCommandParser.Action.LEAVE, leave.action());
    }

    @Test
    void parsesStartDefaultsAndCmiStyleOptions() {
        AdminCounterCommandParser.Request defaults = AdminCounterCommandParser.parse(List.of("start")).orElseThrow();
        AdminCounterCommandParser.Request custom = AdminCounterCommandParser.parse(List.of(
                "start", "r:30", "t:7", "msg:&eCustom_message", "-f"
        )).orElseThrow();

        assertEquals(AdminCounterCommandParser.Action.START, defaults.action());
        assertTrue(defaults.seconds().isEmpty());
        assertTrue(defaults.range().isEmpty());
        assertTrue(defaults.message().isEmpty());
        assertTrue(!defaults.force());

        assertEquals(7, custom.seconds().orElseThrow());
        assertEquals(30.0, custom.range().orElseThrow());
        assertEquals("&eCustom message", custom.message().orElseThrow());
        assertTrue(custom.force());
    }

    @Test
    void parsesGlobalRangeAndCenterLocation() {
        AdminCounterCommandParser.Request request = AdminCounterCommandParser.parse(List.of(
                "start", "r:-1", "c:world:10:64:-20"
        )).orElseThrow();

        assertEquals(-1.0, request.range().orElseThrow());
        AdminCounterCommandParser.Center center = request.center().orElseThrow();
        assertEquals("world", center.worldName());
        assertEquals(10.0, center.x());
        assertEquals(64.0, center.y());
        assertEquals(-20.0, center.z());
    }

    @Test
    void rejectsMalformedRequests() {
        assertTrue(AdminCounterCommandParser.parse(List.of()).isEmpty());
        assertTrue(AdminCounterCommandParser.parse(List.of("start", "t:0")).isEmpty());
        assertTrue(AdminCounterCommandParser.parse(List.of("start", "t:nope")).isEmpty());
        assertTrue(AdminCounterCommandParser.parse(List.of("start", "r:-2")).isEmpty());
        assertTrue(AdminCounterCommandParser.parse(List.of("start", "c:world:1:2")).isEmpty());
        assertTrue(AdminCounterCommandParser.parse(List.of("start", "-x")).isEmpty());
        assertTrue(AdminCounterCommandParser.parse(List.of("join", "-f")).isEmpty());
    }
}
