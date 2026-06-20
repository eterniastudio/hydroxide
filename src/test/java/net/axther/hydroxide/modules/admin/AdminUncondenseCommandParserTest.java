package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUncondenseCommandParserTest {

    @Test
    void parsesSelfAllAndOptionalMaterialForms() {
        AdminUncondenseCommandParser.Request self = AdminUncondenseCommandParser.parse(List.of()).orElseThrow();
        AdminUncondenseCommandParser.Request all = AdminUncondenseCommandParser.parse(List.of("all")).orElseThrow();
        AdminUncondenseCommandParser.Request filtered = AdminUncondenseCommandParser.parse(List.of("iron_block")).orElseThrow();

        assertTrue(self.materialName().isEmpty());
        assertTrue(self.targetName().isEmpty());
        assertTrue(!self.silent());
        assertTrue(all.materialName().isEmpty());
        assertTrue(all.targetName().isEmpty());
        assertEquals("iron_block", filtered.materialName().orElseThrow());
    }

    @Test
    void parsesTargetAndSilentForms() {
        AdminUncondenseCommandParser.Request targetAll = AdminUncondenseCommandParser.parse(List.of("all", "Alex")).orElseThrow();
        AdminUncondenseCommandParser.Request targetFiltered = AdminUncondenseCommandParser.parse(List.of("iron_ingot", "Alex", "-s")).orElseThrow();

        assertTrue(targetAll.materialName().isEmpty());
        assertEquals("Alex", targetAll.targetName().orElseThrow());
        assertEquals("iron_ingot", targetFiltered.materialName().orElseThrow());
        assertEquals("Alex", targetFiltered.targetName().orElseThrow());
        assertTrue(targetFiltered.silent());
    }

    @Test
    void rejectsUnknownFlagsAndExtraArguments() {
        assertTrue(AdminUncondenseCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(AdminUncondenseCommandParser.parse(List.of("all", "-x")).isEmpty());
        assertTrue(AdminUncondenseCommandParser.parse(List.of("iron_block", "Alex", "-x")).isEmpty());
        assertTrue(AdminUncondenseCommandParser.parse(List.of("iron_block", "Alex", "-s", "extra")).isEmpty());
    }
}
