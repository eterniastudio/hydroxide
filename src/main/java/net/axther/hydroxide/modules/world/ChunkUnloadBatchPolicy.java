package net.axther.hydroxide.modules.world;

final class ChunkUnloadBatchPolicy {

    private ChunkUnloadBatchPolicy() {
    }

    static int limit(int remainingChunks, boolean forced, int configuredBatchSize) {
        if (remainingChunks <= 0) {
            return 0;
        }
        if (forced) {
            return remainingChunks;
        }
        return Math.min(remainingChunks, Math.max(1, configuredBatchSize));
    }
}
