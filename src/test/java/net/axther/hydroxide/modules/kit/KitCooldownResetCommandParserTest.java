package net.axther.hydroxide.modules.kit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitCooldownResetCommandParserTest {

    @Test
    void parsesSinglePlayerReset() {
        KitCooldownResetCommandParser.Request request = KitCooldownResetCommandParser.parse(List.of("starter", "Alex")).orElseThrow();

        assertEquals("starter", request.kit());
        assertEquals("Alex", request.target().name());
        assertFalse(request.target().all());
    }

    @Test
    void parsesAllAliases() {
        assertTrue(KitCooldownResetCommandParser.parse(List.of("starter", "all")).orElseThrow().target().all());
        assertTrue(KitCooldownResetCommandParser.parse(List.of("starter", "*")).orElseThrow().target().all());
        assertTrue(KitCooldownResetCommandParser.parse(List.of("starter", "@a")).orElseThrow().target().all());
    }

    @Test
    void rejectsMissingOrExtraArguments() {
        assertTrue(KitCooldownResetCommandParser.parse(List.of()).isEmpty());
        assertTrue(KitCooldownResetCommandParser.parse(List.of("starter")).isEmpty());
        assertTrue(KitCooldownResetCommandParser.parse(List.of("starter", "Alex", "extra")).isEmpty());
    }
}
