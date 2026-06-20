package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandControlPolicyTest {

    @Test
    void blocksDisabledCommandLabelsWithArgumentsAndNamespaces() {
        CommandControlPolicy policy = new CommandControlPolicy(List.of("nick", "minecraft:me"));

        assertTrue(policy.blocked("/nick Bob", noPermissions()));
        assertTrue(policy.blocked("minecraft:me waves", noPermissions()));
        assertFalse(policy.blocked("/nickname Bob", noPermissions()));
    }

    @Test
    void ignoresSlashAndCaseWhenMatchingDisabledCommands() {
        CommandControlPolicy policy = new CommandControlPolicy(List.of("/Spawn"));

        assertTrue(policy.blocked("/spawn", noPermissions()));
        assertTrue(policy.blocked("SPAWN hub", noPermissions()));
    }

    @Test
    void bypassPermissionAllowsDisabledCommand() {
        CommandControlPolicy policy = new CommandControlPolicy(List.of("spawn"));

        assertFalse(policy.blocked("/spawn", permission -> permission.equals("hydroxide.command.disabled.bypass")));
    }

    private Predicate<String> noPermissions() {
        return permission -> false;
    }
}
