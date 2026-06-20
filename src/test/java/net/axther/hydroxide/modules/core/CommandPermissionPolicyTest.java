package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPermissionPolicyTest {

    @Test
    void blocksWhenRequiredPermissionIsMissing() {
        CommandPermissionPolicy policy = new CommandPermissionPolicy(Map.of(
                "rtp", new CommandPermissionPolicy.Rule(List.of("hydroxide.custom.rtp"), CommandPermissionPolicy.Mode.ANY)
        ));

        CommandPermissionPolicy.MissingPermission missing = policy.missing("/rtp wild", noPermissions()).orElseThrow();

        assertEquals("rtp", missing.key());
        assertEquals("hydroxide.custom.rtp", missing.permissionList());
    }

    @Test
    void allowsWhenAnyPermissionMatches() {
        CommandPermissionPolicy policy = new CommandPermissionPolicy(Map.of(
                "shop", new CommandPermissionPolicy.Rule(List.of("hydroxide.shop.vip", "hydroxide.shop.admin"), CommandPermissionPolicy.Mode.ANY)
        ));

        assertTrue(policy.missing("/shop", permission -> permission.equals("hydroxide.shop.vip")).isEmpty());
    }

    @Test
    void supportsAllMode() {
        CommandPermissionPolicy policy = new CommandPermissionPolicy(Map.of(
                "eco-set", new CommandPermissionPolicy.Rule(List.of("hydroxide.eco", "hydroxide.eco.set"), CommandPermissionPolicy.Mode.ALL)
        ));

        assertTrue(policy.missing("/eco set Alex 10", permission -> permission.equals("hydroxide.eco")).isPresent());
        assertTrue(policy.missing("/eco set Alex 10", permission -> permission.startsWith("hydroxide.eco")).isEmpty());
    }

    @Test
    void prefersMostSpecificSubcommandRule() {
        CommandPermissionPolicy policy = new CommandPermissionPolicy(Map.of(
                "kit", new CommandPermissionPolicy.Rule(List.of("hydroxide.kit"), CommandPermissionPolicy.Mode.ANY),
                "kit-tools", new CommandPermissionPolicy.Rule(List.of("hydroxide.kit.tools"), CommandPermissionPolicy.Mode.ANY)
        ));

        assertEquals("kit-tools", policy.missing("/kit tools", noPermissions()).orElseThrow().key());
    }

    @Test
    void bypassPermissionSkipsChecks() {
        CommandPermissionPolicy policy = new CommandPermissionPolicy(Map.of(
                "home", new CommandPermissionPolicy.Rule(List.of("hydroxide.home.custom"), CommandPermissionPolicy.Mode.ANY)
        ));

        assertTrue(policy.missing("/home base", permission -> permission.equals("hydroxide.command.permission.bypass")).isEmpty());
    }

    private Predicate<String> noPermissions() {
        return permission -> false;
    }
}
