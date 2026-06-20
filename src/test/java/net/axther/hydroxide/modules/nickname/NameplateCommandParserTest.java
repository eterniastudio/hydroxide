package net.axther.hydroxide.modules.nickname;

import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameplateCommandParserTest {

    @Test
    void parsesTargetedPrefixSuffixColorAndSilentFlags() {
        NameplateCommandParser.Request request = NameplateCommandParser.parse(List.of(
                "Alex",
                "-pref:&6[Admin] ",
                "-suf:<gray>*",
                "-c:red",
                "-s"
        )).orElseThrow();

        assertEquals("Alex", request.targetName().orElseThrow());
        assertEquals("&6[Admin] ", request.prefix().orElseThrow());
        assertEquals("<gray>*", request.suffix().orElseThrow());
        assertTrue(request.colorProvided());
        assertEquals(NamedTextColor.RED, request.color().orElseThrow());
        assertTrue(request.silent());
        assertFalse(request.reset());
    }

    @Test
    void parsesSelfUpdateResetAndLegacyColorCodes() {
        NameplateCommandParser.Request self = NameplateCommandParser.parse(List.of("-pref:<#44CCFF>VIP")).orElseThrow();
        NameplateCommandParser.Request reset = NameplateCommandParser.parse(List.of("Steve", "reset", "-s")).orElseThrow();
        NameplateCommandParser.Request legacy = NameplateCommandParser.parse(List.of("-c:&a")).orElseThrow();

        assertTrue(self.targetName().isEmpty());
        assertEquals("<#44CCFF>VIP", self.prefix().orElseThrow());
        assertEquals("Steve", reset.targetName().orElseThrow());
        assertTrue(reset.reset());
        assertTrue(reset.silent());
        assertEquals(NamedTextColor.GREEN, legacy.color().orElseThrow());
    }

    @Test
    void rejectsInvalidOrAmbiguousShapes() {
        assertTrue(NameplateCommandParser.parse(List.of()).isEmpty());
        assertTrue(NameplateCommandParser.parse(List.of("Alex")).isEmpty());
        assertTrue(NameplateCommandParser.parse(List.of("-x:test")).isEmpty());
        assertTrue(NameplateCommandParser.parse(List.of("-c:#123456")).isEmpty());
        assertTrue(NameplateCommandParser.parse(List.of("reset", "-pref:VIP")).isEmpty());
    }
}
