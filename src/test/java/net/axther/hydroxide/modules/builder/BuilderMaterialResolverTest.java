package net.axther.hydroxide.modules.builder;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderMaterialResolverTest {

    @Test
    void parsesRegistryStyleMaterialKeys() {
        assertEquals(Material.STONE, BuilderMaterialResolver.resolve("minecraft:stone").orElseThrow());
        assertEquals(Material.OAK_PLANKS, BuilderMaterialResolver.resolve("oak planks").orElseThrow());
    }

    @Test
    void rejectsUnknownOrNonBlockMaterials() {
        assertTrue(BuilderMaterialResolver.resolve("made_up_block").isEmpty());
        assertTrue(BuilderMaterialResolver.resolveBlock("diamond_sword").isEmpty());
    }
}
