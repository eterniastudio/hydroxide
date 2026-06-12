package net.axther.hydroxide.modules.interaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandBindingTest {

    @Test
    void parsesCommandSignLinesWithCostAndCooldown() {
        CommandBinding binding = CommandBinding.fromSignLines(List.of("[Command]", "console:say {player}", "cost:12.50", "cooldown:30")).orElseThrow();

        assertEquals(CommandBinding.ExecutionMode.CONSOLE, binding.mode());
        assertEquals("say {player}", binding.command());
        assertEquals(12.50D, binding.cost());
        assertEquals(30, binding.cooldownSeconds());
        assertTrue(binding.readyAt(1_000L, 31_000L));
    }
}
