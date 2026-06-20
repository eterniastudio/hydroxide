package net.axther.hydroxide.modules.shop;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorthGenerationPlannerTest {

    @Test
    void generatesOnlyItemMaterialsAndSkipsExistingWithoutOverwrite() {
        WorthGenerationPlanner.Plan plan = WorthGenerationPlanner.plan(
                List.of(Material.AIR, Material.STONE, Material.DIAMOND),
                Set.of("STONE"),
                1.0D,
                false
        );

        assertFalse(plan.prices().containsKey(Material.AIR));
        assertFalse(plan.prices().containsKey(Material.STONE));
        assertTrue(plan.prices().containsKey(Material.DIAMOND));
        assertEquals(1, plan.created());
        assertEquals(0, plan.overwritten());
        assertEquals(1, plan.skippedExisting());
    }

    @Test
    void overwritesExistingWhenRequested() {
        WorthGenerationPlanner.Plan plan = WorthGenerationPlanner.plan(
                List.of(Material.STONE, Material.DIAMOND),
                Set.of("stone"),
                2.0D,
                true
        );

        assertTrue(plan.prices().containsKey(Material.STONE));
        assertTrue(plan.prices().containsKey(Material.DIAMOND));
        assertEquals(1, plan.created());
        assertEquals(1, plan.overwritten());
        assertEquals(0, plan.skippedExisting());
    }
}
