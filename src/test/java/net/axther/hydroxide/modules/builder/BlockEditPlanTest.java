package net.axther.hydroxide.modules.builder;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockEditPlanTest {

    @Test
    void batchesChangesDeterministically() {
        BlockEditPlan plan = new BlockEditPlan(List.of(
                new BlockChange(new BlockVector3i(0, 0, 0), Material.AIR, Material.STONE),
                new BlockChange(new BlockVector3i(1, 0, 0), Material.AIR, Material.STONE),
                new BlockChange(new BlockVector3i(2, 0, 0), Material.AIR, Material.STONE)
        ));

        assertEquals(2, plan.batches(2).size());
        assertEquals(2, plan.batches(2).get(0).size());
        assertEquals(1, plan.batches(2).get(1).size());
    }
}
