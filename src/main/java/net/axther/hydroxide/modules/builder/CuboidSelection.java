package net.axther.hydroxide.modules.builder;

import java.util.Locale;

public record CuboidSelection(String worldName, BlockVector3i first, BlockVector3i second) {

    public BlockVector3i min() {
        return new BlockVector3i(
                Math.min(first.x(), second.x()),
                Math.min(first.y(), second.y()),
                Math.min(first.z(), second.z())
        );
    }

    public BlockVector3i max() {
        return new BlockVector3i(
                Math.max(first.x(), second.x()),
                Math.max(first.y(), second.y()),
                Math.max(first.z(), second.z())
        );
    }

    public long volume() {
        BlockVector3i min = min();
        BlockVector3i max = max();
        return (long) (max.x() - min.x() + 1)
                * (max.y() - min.y() + 1)
                * (max.z() - min.z() + 1);
    }

    public boolean withinLimit(long maxBlocks) {
        return volume() <= maxBlocks;
    }

    public static boolean validWorld(String firstWorld, String secondWorld) {
        if (firstWorld == null || secondWorld == null) {
            return false;
        }
        return firstWorld.toLowerCase(Locale.ROOT).equals(secondWorld.toLowerCase(Locale.ROOT));
    }
}
