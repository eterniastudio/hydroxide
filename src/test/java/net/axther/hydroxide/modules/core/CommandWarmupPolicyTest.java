package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandWarmupPolicyTest {

    @Test
    void matchesCommandLabelIgnoringSlashAndCase() {
        CommandWarmupPolicy policy = new CommandWarmupPolicy(Map.of("Spawn", Duration.ofSeconds(3)));

        assertEquals(Duration.ofSeconds(3), policy.warmup("/spawn hub", noPermissions()).orElseThrow().duration());
        assertEquals(Duration.ofSeconds(3), policy.warmup("SPAWN hub", noPermissions()).orElseThrow().duration());
    }

    @Test
    void prefersMostSpecificSubcommandWarmup() {
        CommandWarmupPolicy policy = new CommandWarmupPolicy(Map.of(
                "warp", Duration.ofSeconds(2),
                "warp-pvp", Duration.ofSeconds(8)
        ));

        assertEquals(Duration.ofSeconds(8), policy.warmup("/warp pvp", noPermissions()).orElseThrow().duration());
        assertEquals(Duration.ofSeconds(2), policy.warmup("/warp market", noPermissions()).orElseThrow().duration());
    }

    @Test
    void bypassPermissionSkipsWarmup() {
        CommandWarmupPolicy policy = new CommandWarmupPolicy(Map.of("home", Duration.ofSeconds(10)));

        assertTrue(policy.warmup("/home base", permission -> permission.equals("hydroxide.command.warmup.bypass")).isEmpty());
    }

    private Predicate<String> noPermissions() {
        return permission -> false;
    }
}
