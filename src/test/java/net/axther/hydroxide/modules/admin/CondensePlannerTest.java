package net.axther.hydroxide.modules.admin;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CondensePlannerTest {

    @Test
    void condensesNineToOneBlockRecipesAndKeepsRemainders() {
        CondensePlanner.Plan plan = CondensePlanner.plan(Map.of(
                Material.IRON_INGOT, 20,
                Material.DIAMOND, 9,
                Material.STONE, 64
        ));

        assertEquals(2, plan.outputs().get(Material.IRON_BLOCK));
        assertEquals(2, plan.remainders().get(Material.IRON_INGOT));
        assertEquals(1, plan.outputs().get(Material.DIAMOND_BLOCK));
        assertTrue(plan.remainders().containsKey(Material.STONE));
        assertEquals(27, plan.consumed());
        assertEquals(3, plan.produced());
    }

    @Test
    void condensesFourToOneRecipes() {
        CondensePlanner.Plan plan = CondensePlanner.plan(Map.of(
                Material.GLOWSTONE_DUST, 11,
                Material.SNOWBALL, 4
        ));

        assertEquals(2, plan.outputs().get(Material.GLOWSTONE));
        assertEquals(3, plan.remainders().get(Material.GLOWSTONE_DUST));
        assertEquals(1, plan.outputs().get(Material.SNOW_BLOCK));
        assertEquals(12, plan.consumed());
        assertEquals(3, plan.produced());
    }

    @Test
    void filtersToRequestedSourceMaterial() {
        CondensePlanner.Plan plan = CondensePlanner.plan(Map.of(
                Material.IRON_INGOT, 18,
                Material.GOLD_INGOT, 18
        ), Material.GOLD_INGOT);

        assertTrue(plan.outputs().containsKey(Material.GOLD_BLOCK));
        assertTrue(plan.remainders().containsKey(Material.IRON_INGOT));
        assertFalse(plan.outputs().containsKey(Material.IRON_BLOCK));
    }

    @Test
    void uncondensesCompactItemsIntoIngredientMaterials() {
        CondensePlanner.Plan plan = CondensePlanner.uncondense(Map.of(
                Material.IRON_BLOCK, 2,
                Material.GLOWSTONE, 3,
                Material.STONE, 64
        ), null);

        assertEquals(18, plan.outputs().get(Material.IRON_INGOT));
        assertEquals(12, plan.outputs().get(Material.GLOWSTONE_DUST));
        assertTrue(plan.remainders().containsKey(Material.STONE));
        assertEquals(5, plan.consumed());
        assertEquals(30, plan.produced());
    }

    @Test
    void uncondenseFilterAcceptsCompactOrIngredientMaterial() {
        CondensePlanner.Plan byBlock = CondensePlanner.uncondense(Map.of(
                Material.IRON_BLOCK, 2,
                Material.GOLD_BLOCK, 2
        ), Material.IRON_BLOCK);
        CondensePlanner.Plan byIngredient = CondensePlanner.uncondense(Map.of(
                Material.IRON_BLOCK, 2,
                Material.GOLD_BLOCK, 2
        ), Material.IRON_INGOT);

        assertEquals(Map.of(Material.IRON_INGOT, 18), byBlock.outputs());
        assertEquals(Map.of(Material.IRON_INGOT, 18), byIngredient.outputs());
        assertTrue(byBlock.remainders().containsKey(Material.GOLD_BLOCK));
        assertTrue(byIngredient.remainders().containsKey(Material.GOLD_BLOCK));
    }

    @Test
    void reportsSupportedCompactOrIngredientMaterialsForUncondense() {
        assertTrue(CondensePlanner.supportedUncondenseMaterials().contains(Material.IRON_BLOCK));
        assertTrue(CondensePlanner.supportedUncondenseMaterials().contains(Material.IRON_INGOT));
        assertTrue(CondensePlanner.supportedUncondenseMaterials().contains(Material.GLOWSTONE));
        assertTrue(CondensePlanner.supportedUncondenseMaterials().contains(Material.GLOWSTONE_DUST));
    }

    @Test
    void reportsSupportedSourceMaterials() {
        assertTrue(CondensePlanner.supportedSources().contains(Material.IRON_INGOT));
        assertTrue(CondensePlanner.supportedSources().contains(Material.REDSTONE));
        assertTrue(CondensePlanner.supportedSources().contains(Material.GLOWSTONE_DUST));
    }
}
