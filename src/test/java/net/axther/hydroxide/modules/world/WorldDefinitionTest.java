package net.axther.hydroxide.modules.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldDefinitionTest {

    @Test
    void normalizesEnvironmentAndSeedInput() {
        WorldDefinition definition = WorldDefinition.fromCommand("sky", "nether", "12345", "flat");

        assertEquals("sky", definition.name());
        assertEquals("NETHER", definition.environment());
        assertEquals(12345L, definition.seed());
        assertEquals("flat", definition.generator());
    }
}
