package net.axther.hydroxide.modules.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KickTargetPolicyTest {

    @Test
    void excludesTargetsWithKickBypassPermission() {
        KickTargetPolicy.Selection<String> selection = KickTargetPolicy.selectKickable(
                List.of("Alex", "Mod", "Steve"),
                Set.of("Mod")::contains
        );

        assertEquals(List.of("Alex", "Steve"), selection.kickable());
        assertEquals(List.of("Mod"), selection.bypassed());
    }

    @Test
    void keepsAllTargetsWhenNoBypassMatches() {
        KickTargetPolicy.Selection<String> selection = KickTargetPolicy.selectKickable(
                List.of("Alex", "Steve"),
                name -> false
        );

        assertEquals(List.of("Alex", "Steve"), selection.kickable());
        assertEquals(List.of(), selection.bypassed());
    }
}
