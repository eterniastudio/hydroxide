package net.axther.hydroxide.modules.utility;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

final class BlockInfoFormatter {

    private BlockInfoFormatter() {
    }

    static Details details(Block block) {
        Location location = block.getLocation();
        return new Details(
                block.getType().name(),
                block.getType().key().asString(),
                location(location),
                block.getLightLevel(),
                biome(block),
                solid(block.getType())
        );
    }

    private static boolean solid(Material material) {
        try {
            return material.isSolid();
        } catch (LinkageError | RuntimeException exception) {
            return false;
        }
    }

    private static String biome(Block block) {
        try {
            Biome biome = block.getBiome();
            return biome == null ? "unknown" : biome.key().value();
        } catch (LinkageError | RuntimeException exception) {
            return "unknown";
        }
    }

    private static String location(Location location) {
        String world = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        return world + ":" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    record Details(String material, String key, String location, int light, String biome, boolean solid) {
    }
}
