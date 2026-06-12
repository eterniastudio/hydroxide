package net.axther.hydroxide.registry;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernRegistryKeysTest {

    @Test
    void normalizesMinecraftRegistryKeysWithoutDeprecatedEnumNames() {
        assertEquals("attack_damage", ModernRegistryKeys.minecraftKey("ATTACK_DAMAGE"));
        assertEquals("block.note_block.chime", ModernRegistryKeys.minecraftKey("minecraft:block.note_block.chime"));
    }

    @Test
    void offersLegacySoundNameCandidatesForRegistryLookup() {
        assertTrue(ModernRegistryKeys.soundKeys("BLOCK_NOTE_BLOCK_CHIME").contains("block.note_block.chime"));
    }

    @Test
    void offersSnakeCaseGameRuleCandidatesFromCamelCaseInput() {
        assertTrue(ModernRegistryKeys.gameRuleKeys("doDaylightCycle").contains("do_daylight_cycle"));
    }

    @Test
    void parsesTypedGameRuleValuesFromExistingDefaultType() {
        assertEquals(Optional.of(Boolean.TRUE), ModernRegistryKeys.parseGameRuleValue(Boolean.FALSE, "true"));
        assertEquals(Optional.of(12), ModernRegistryKeys.parseGameRuleValue(0, "12"));
        assertEquals(Optional.empty(), ModernRegistryKeys.parseGameRuleValue(0, "not-a-number"));
    }
}
