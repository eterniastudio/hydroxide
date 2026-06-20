package net.axther.hydroxide.modules.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandSpyFormatterTest {

    @Test
    void stripsLeadingSlashForDisplay() {
        assertEquals("warp spawn", CommandSpyFormatter.displayCommand("/warp spawn"));
        assertEquals("warp spawn", CommandSpyFormatter.displayCommand("warp spawn"));
    }

    @Test
    void skipsConfiguredSensitivePrefixes() {
        List<String> prefixes = List.of("login", "register", "changepassword");

        assertTrue(CommandSpyFormatter.shouldSkip("/login hunter2", prefixes));
        assertTrue(CommandSpyFormatter.shouldSkip("/register hunter2 hunter2", prefixes));
        assertFalse(CommandSpyFormatter.shouldSkip("/warp login", prefixes));
    }
}
