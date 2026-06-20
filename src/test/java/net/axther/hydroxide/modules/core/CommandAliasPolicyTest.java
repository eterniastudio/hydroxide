package net.axther.hydroxide.modules.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandAliasPolicyTest {

    @Test
    void rewritesAliasIgnoringSlashAndCaseAndAppendsArguments() {
        CommandAliasPolicy policy = new CommandAliasPolicy(Map.of("Hub", "warp hub"));

        assertEquals("/warp hub fast", policy.rewrite("/HUB fast", "Alex").orElseThrow());
    }

    @Test
    void replacesExplicitArgsPlaceholderWithoutAppendingTwice() {
        CommandAliasPolicy policy = new CommandAliasPolicy(Map.of("rawsay", "broadcast {args}"));

        assertEquals("/broadcast hello world", policy.rewrite("/rawsay hello world", "Alex").orElseThrow());
    }

    @Test
    void replacesPlayerPlaceholder() {
        CommandAliasPolicy policy = new CommandAliasPolicy(Map.of("homebase", "home {player}-base"));

        assertEquals("/home Alex-base", policy.rewrite("/homebase", "Alex").orElseThrow());
    }

    @Test
    void ignoresUnknownOrSelfReferentialAliases() {
        CommandAliasPolicy policy = new CommandAliasPolicy(Map.of(
                "hub", "warp hub",
                "spawn", "spawn"
        ));

        assertTrue(policy.rewrite("/unknown value", "Alex").isEmpty());
        assertTrue(policy.rewrite("/spawn", "Alex").isEmpty());
    }
}
