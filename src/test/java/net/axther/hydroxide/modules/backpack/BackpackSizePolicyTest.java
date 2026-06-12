package net.axther.hydroxide.modules.backpack;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackpackSizePolicyTest {

    @Test
    void choosesLargestAllowedNineAlignedSizeWithinBounds() {
        BackpackSizePolicy policy = new BackpackSizePolicy(27, 54);

        assertEquals(27, policy.slotsFor(Set.of()));
        assertEquals(45, policy.slotsFor(Set.of("hydroxide.backpack.size.18", "hydroxide.backpack.size.45")));
        assertEquals(54, policy.slotsFor(Set.of("hydroxide.backpack.size.90")));
        assertEquals(9, new BackpackSizePolicy(4, 54).slotsFor(Set.of()));
    }
}
