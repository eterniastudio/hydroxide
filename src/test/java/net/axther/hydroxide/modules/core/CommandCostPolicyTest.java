package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCostPolicyTest {

    @Test
    void matchesCommandLabelCostIgnoringSlashAndCase() {
        CommandCostPolicy policy = new CommandCostPolicy(Map.of("Spawn", 25.0));

        assertEquals(25.0, policy.cost("/spawn hub", noPermissions()).orElseThrow().amount());
        assertEquals(25.0, policy.cost("SPAWN hub", noPermissions()).orElseThrow().amount());
    }

    @Test
    void prefersMostSpecificSubcommandCost() {
        CommandCostPolicy policy = new CommandCostPolicy(Map.of(
                "kit", 5.0,
                "kit-tools", 15.0
        ));

        assertEquals(15.0, policy.cost("/kit tools", noPermissions()).orElseThrow().amount());
        assertEquals(5.0, policy.cost("/kit food", noPermissions()).orElseThrow().amount());
    }

    @Test
    void bypassPermissionSkipsCommandCost() {
        CommandCostPolicy policy = new CommandCostPolicy(Map.of("home", 10.0));

        assertTrue(policy.cost("/home base", permission -> permission.equals("hydroxide.command.cost.bypass")).isEmpty());
    }

    @Test
    void ignoresInvalidOrFreeCosts() {
        CommandCostPolicy policy = new CommandCostPolicy(Map.of(
                "free", 0.0,
                "broken", Double.NaN
        ));

        assertTrue(policy.cost("/free", noPermissions()).isEmpty());
        assertTrue(policy.cost("/broken", noPermissions()).isEmpty());
    }

    private Predicate<String> noPermissions() {
        return permission -> false;
    }
}
