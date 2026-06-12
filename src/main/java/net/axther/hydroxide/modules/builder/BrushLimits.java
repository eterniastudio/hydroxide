package net.axther.hydroxide.modules.builder;

public record BrushLimits(int maxRadius, int maxBlocks) {

    public int clampRadius(int requestedRadius) {
        return Math.max(1, Math.min(maxRadius, requestedRadius));
    }

    public boolean withinBlockLimit(int blocks) {
        return blocks <= maxBlocks;
    }
}
