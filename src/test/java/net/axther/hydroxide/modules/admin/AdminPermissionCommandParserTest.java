package net.axther.hydroxide.modules.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPermissionCommandParserTest {

    @Test
    void parsesOptionalCheckPermissionKeyword() {
        assertTrue(AdminPermissionCommandParser.parseCheckPerm(List.of()).orElseThrow().keyword().isEmpty());

        AdminPermissionCommandParser.CheckPermRequest request = AdminPermissionCommandParser
                .parseCheckPerm(List.of("admin.give"))
                .orElseThrow();

        assertEquals("admin.give", request.keyword().orElseThrow());
    }

    @Test
    void rejectsCheckPermWithTooManyArguments() {
        assertTrue(AdminPermissionCommandParser.parseCheckPerm(List.of("admin", "give")).isEmpty());
    }

    @Test
    void parsesHasPermissionPlayerAndNode() {
        AdminPermissionCommandParser.HasPermissionRequest request = AdminPermissionCommandParser
                .parseHasPermission(List.of("Steve", "hydroxide.admin.give"))
                .orElseThrow();

        assertEquals("Steve", request.playerName());
        assertEquals("hydroxide.admin.give", request.permission());
    }

    @Test
    void rejectsHasPermissionWithMissingArguments() {
        assertTrue(AdminPermissionCommandParser.parseHasPermission(List.of()).isEmpty());
        assertTrue(AdminPermissionCommandParser.parseHasPermission(List.of("Steve")).isEmpty());
    }
}
