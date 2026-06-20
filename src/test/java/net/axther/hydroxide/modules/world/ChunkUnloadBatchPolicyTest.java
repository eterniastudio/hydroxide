package net.axther.hydroxide.modules.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkUnloadBatchPolicyTest {

    @Test
    void forcedModeProcessesAllEligibleChunksAtOnce() {
        assertEquals(250, ChunkUnloadBatchPolicy.limit(250, true, 25));
    }

    @Test
    void regularModeUsesConfiguredBatchSizeAndKeepsAtLeastOneChunk() {
        assertEquals(25, ChunkUnloadBatchPolicy.limit(250, false, 25));
        assertEquals(1, ChunkUnloadBatchPolicy.limit(250, false, 0));
        assertEquals(7, ChunkUnloadBatchPolicy.limit(7, false, 25));
    }

    @Test
    void emptyWorkHasZeroLimit() {
        assertEquals(0, ChunkUnloadBatchPolicy.limit(0, false, 25));
        assertEquals(0, ChunkUnloadBatchPolicy.limit(0, true, 25));
    }
}
