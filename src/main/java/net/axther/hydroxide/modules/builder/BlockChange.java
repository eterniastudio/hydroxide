package net.axther.hydroxide.modules.builder;

import org.bukkit.Material;

public record BlockChange(BlockVector3i position, Material before, Material after) {

    public BlockChange inverse() {
        return new BlockChange(position, after, before);
    }
}
