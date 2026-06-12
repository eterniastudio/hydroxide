package net.axther.hydroxide.modules.builder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuboidSelectionTest {

    @Test
    void normalizesCornersAndCalculatesVolume() {
        CuboidSelection selection = new CuboidSelection("world", new BlockVector3i(5, 70, 2), new BlockVector3i(3, 68, 4));

        assertEquals(new BlockVector3i(3, 68, 2), selection.min());
        assertEquals(new BlockVector3i(5, 70, 4), selection.max());
        assertEquals(27, selection.volume());
    }

    @Test
    void validatesVolumeLimitsAndWorldMatch() {
        CuboidSelection selection = new CuboidSelection("world", new BlockVector3i(0, 0, 0), new BlockVector3i(9, 9, 9));

        assertTrue(selection.withinLimit(1000));
        assertFalse(selection.withinLimit(999));
        assertFalse(CuboidSelection.validWorld("world", "world_nether"));
    }
}
