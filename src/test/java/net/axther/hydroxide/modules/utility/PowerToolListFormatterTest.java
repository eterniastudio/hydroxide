package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerToolListFormatterTest {

    @Test
    void createsPlaceholdersForBoundPowerTool() {
        AttachedCommand command = new AttachedCommand(AttachedCommand.Click.RIGHT, AttachedCommand.Executor.PLAYER, -1, "warp spawn");

        Map<String, Object> placeholders = PowerToolListFormatter.placeholders(command);

        assertEquals("right", placeholders.get("click"));
        assertEquals("player", placeholders.get("executor"));
        assertEquals("infinite", placeholders.get("uses"));
        assertEquals("warp spawn", placeholders.get("command"));
    }

    @Test
    void formatsFiniteUses() {
        AttachedCommand command = new AttachedCommand(AttachedCommand.Click.LEFT, AttachedCommand.Executor.CONSOLE, 3, "say hi");

        assertEquals(3, PowerToolListFormatter.placeholders(command).get("uses"));
    }
}
