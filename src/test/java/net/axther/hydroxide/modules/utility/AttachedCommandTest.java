package net.axther.hydroxide.modules.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachedCommandTest {

    @Test
    void consoleExecutionRequiresExplicitPermission() {
        AttachedCommand command = AttachedCommand.parse("right console infinite say {player}").orElseThrow();

        assertFalse(command.canAttach(false));
        assertTrue(command.canAttach(true));
    }

    @Test
    void parsesLimitedUsesAndSafePlaceholders() {
        AttachedCommand command = AttachedCommand.parse("left player 3 home {world}").orElseThrow();

        assertTrue(command.consumeUse().isPresent());
        assertTrue(command.render("Steve", "uuid", "world", 1, 2, 3).contains("Steve") == false);
        assertTrue(command.render("Steve", "uuid", "world", 1, 2, 3).contains("world"));
    }
}
