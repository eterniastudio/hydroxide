package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminTreeCommandParserTest {

    @Test
    void parsesDefaultsAndTreeType() {
        AdminTreeCommandParser.Request defaults = AdminTreeCommandParser.parse(List.of()).orElseThrow();
        AdminTreeCommandParser.Request typed = AdminTreeCommandParser.parse(List.of("big_tree")).orElseThrow();

        assertTrue(defaults.treeTypeName().isEmpty());
        assertTrue(defaults.targetName().isEmpty());
        assertEquals("big_tree", typed.treeTypeName().orElseThrow());
    }

    @Test
    void appliesCommandDefaultWhenTreeTypeIsOmitted() {
        AdminTreeCommandParser.Request defaults = AdminTreeCommandParser.parse(List.of(), "big_tree").orElseThrow();
        AdminTreeCommandParser.Request explicit = AdminTreeCommandParser.parse(List.of("mangrove"), "big_tree").orElseThrow();

        assertEquals("big_tree", defaults.treeTypeName().orElseThrow());
        assertTrue(defaults.targetName().isEmpty());
        assertEquals("mangrove", explicit.treeTypeName().orElseThrow());
    }

    @Test
    void parsesTargetPlayerFlag() {
        AdminTreeCommandParser.Request targetOnly = AdminTreeCommandParser.parse(List.of("-p:Alex")).orElseThrow();
        AdminTreeCommandParser.Request typedTarget = AdminTreeCommandParser.parse(List.of("mangrove", "-p:Alex")).orElseThrow();

        assertTrue(targetOnly.treeTypeName().isEmpty());
        assertEquals("Alex", targetOnly.targetName().orElseThrow());
        assertEquals("mangrove", typedTarget.treeTypeName().orElseThrow());
        assertEquals("Alex", typedTarget.targetName().orElseThrow());
    }

    @Test
    void rejectsMalformedRequests() {
        assertTrue(AdminTreeCommandParser.parse(List.of("-x")).isEmpty());
        assertTrue(AdminTreeCommandParser.parse(List.of("-p:")).isEmpty());
        assertTrue(AdminTreeCommandParser.parse(List.of("oak", "Alex")).isEmpty());
        assertTrue(AdminTreeCommandParser.parse(List.of("oak", "-p:Alex", "extra")).isEmpty());
        assertTrue(AdminTreeCommandParser.parse(List.of("oak", "-p:Alex", "-p:Steve")).isEmpty());
    }
}
