package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandWorldPolicyTest {

    @Test
    void allowsCommandOnlyInConfiguredAllowedWorlds() {
        CommandWorldPolicy policy = new CommandWorldPolicy(Map.of(
                "spawn", new CommandWorldPolicy.Rule(Set.of("hub"), Set.of())
        ));

        assertTrue(policy.restriction("/spawn", "hub", noPermissions()).isEmpty());

        CommandWorldPolicy.Restriction restriction = policy.restriction("/spawn", "survival", noPermissions()).orElseThrow();
        assertEquals("spawn", restriction.key());
        assertEquals("survival", restriction.world());
    }

    @Test
    void blocksCommandInConfiguredBlockedWorlds() {
        CommandWorldPolicy policy = new CommandWorldPolicy(Map.of(
                "rtp", new CommandWorldPolicy.Rule(Set.of(), Set.of("spawn"))
        ));

        assertTrue(policy.restriction("/rtp", "wild", noPermissions()).isEmpty());

        CommandWorldPolicy.Restriction restriction = policy.restriction("/rtp", "spawn", noPermissions()).orElseThrow();
        assertEquals("rtp", restriction.key());
        assertEquals("spawn", restriction.world());
    }

    @Test
    void prefersMostSpecificSubcommandWorldRule() {
        CommandWorldPolicy policy = new CommandWorldPolicy(Map.of(
                "kit", new CommandWorldPolicy.Rule(Set.of("survival"), Set.of()),
                "kit-tools", new CommandWorldPolicy.Rule(Set.of("spawn"), Set.of())
        ));

        assertTrue(policy.restriction("/kit food", "survival", noPermissions()).isEmpty());
        assertTrue(policy.restriction("/kit tools", "spawn", noPermissions()).isEmpty());
        assertEquals("kit-tools", policy.restriction("/kit tools", "survival", noPermissions()).orElseThrow().key());
    }

    @Test
    void bypassPermissionSkipsWorldRestriction() {
        CommandWorldPolicy policy = new CommandWorldPolicy(Map.of(
                "home", new CommandWorldPolicy.Rule(Set.of("survival"), Set.of())
        ));

        assertTrue(policy.restriction("/home base", "spawn", permission -> permission.equals("hydroxide.command.world-restriction.bypass")).isEmpty());
    }

    private Predicate<String> noPermissions() {
        return permission -> false;
    }
}
