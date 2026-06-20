package net.axther.hydroxide.modules.shop;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class WorthGenerationPlanner {

    private WorthGenerationPlanner() {
    }

    static Plan plan(Collection<Material> materials, Set<String> existingKeys, double basePrice, boolean overwrite) {
        Map<Material, Double> generated = new LinkedHashMap<>();
        int skippedExisting = 0;
        int overwritten = 0;
        Set<String> normalizedExisting = existingKeys.stream()
                .map(key -> key.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        for (Material material : materials.stream().sorted(Comparator.comparing(Material::name)).toList()) {
            if (!candidate(material)) {
                continue;
            }
            boolean exists = normalizedExisting.contains(material.name());
            if (exists && !overwrite) {
                skippedExisting++;
                continue;
            }
            if (exists) {
                overwritten++;
            }
            generated.put(material, generatedPrice(material, basePrice));
        }
        return new Plan(generated, generated.size() - overwritten, overwritten, skippedExisting);
    }

    static boolean candidate(Material material) {
        String name = material.name();
        return !Set.of(
                "AIR",
                "CAVE_AIR",
                "VOID_AIR",
                "WATER",
                "LAVA",
                "FIRE",
                "SOUL_FIRE",
                "NETHER_PORTAL",
                "END_PORTAL",
                "END_GATEWAY",
                "BUBBLE_COLUMN",
                "MOVING_PISTON",
                "PISTON_HEAD",
                "REDSTONE_WIRE"
        ).contains(name)
                && !name.endsWith("_WALL_SIGN")
                && !name.endsWith("_WALL_HANGING_SIGN")
                && !name.endsWith("_WALL_HEAD")
                && !name.endsWith("_WALL_SKULL")
                && !name.endsWith("_WALL_TORCH")
                && !name.endsWith("_WALL_BANNER")
                && !name.endsWith("_STEM")
                && !name.endsWith("_PLANT");
    }

    private static double generatedPrice(Material material, double basePrice) {
        String name = material.name();
        double multiplier = 1.0D;
        multiplier += Math.min(4, name.split("_").length - 1) * 0.15D;
        if (looksLikeBlock(name)) {
            multiplier *= 0.85D;
        }
        multiplier *= rarityMultiplier(name);
        return BigDecimal.valueOf(basePrice * multiplier)
                .setScale(2, RoundingMode.HALF_UP)
                .max(BigDecimal.valueOf(0.01D))
                .doubleValue();
    }

    private static boolean looksLikeBlock(String name) {
        return name.endsWith("_BLOCK")
                || name.endsWith("_ORE")
                || name.endsWith("_LOG")
                || name.endsWith("_PLANKS")
                || name.endsWith("_STONE")
                || name.endsWith("_DIRT")
                || name.endsWith("_SAND")
                || name.endsWith("_SLAB")
                || name.endsWith("_STAIRS")
                || name.endsWith("_WALL")
                || name.endsWith("_FENCE")
                || name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR");
    }

    private static double rarityMultiplier(String name) {
        if (name.contains("NETHERITE")) {
            return 80.0D;
        }
        if (name.contains("DIAMOND")) {
            return 35.0D;
        }
        if (name.contains("EMERALD")) {
            return 30.0D;
        }
        if (name.contains("ELYTRA") || name.contains("TOTEM") || name.contains("DRAGON") || name.contains("BEACON")) {
            return 45.0D;
        }
        if (name.contains("SPAWN_EGG")) {
            return 25.0D;
        }
        if (name.contains("GOLD") || name.contains("GILDED")) {
            return 12.0D;
        }
        if (name.contains("IRON")) {
            return 6.0D;
        }
        if (name.contains("COPPER")) {
            return 3.0D;
        }
        if (name.contains("REDSTONE") || name.contains("LAPIS") || name.contains("QUARTZ")) {
            return 2.0D;
        }
        if (name.contains("COAL") || name.contains("AMETHYST")) {
            return 1.5D;
        }
        return 1.0D;
    }

    record Plan(Map<Material, Double> prices, int created, int overwritten, int skippedExisting) {
    }
}
