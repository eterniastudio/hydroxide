package net.axther.hydroxide.modules.builder;

import net.axther.hydroxide.registry.ModernRegistryKeys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import java.util.Optional;

public final class BuilderMaterialResolver {

    private BuilderMaterialResolver() {
    }

    public static Optional<Material> resolve(String input) {
        String key = ModernRegistryKeys.minecraftKey(input);
        try {
            Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(key));
            if (material != null) {
                return Optional.of(material);
            }
        } catch (Throwable ignored) {
            // Paper registries are unavailable in plain unit tests; runtime servers use the registry path above.
        }
        try {
            return Optional.of(Material.valueOf(key.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static Optional<Material> resolveBlock(String input) {
        Optional<Material> material = resolve(input);
        if (material.isEmpty()) {
            return Optional.empty();
        }
        try {
            return material.filter(Material::isBlock);
        } catch (Throwable ignored) {
            String key = ModernRegistryKeys.minecraftKey(input);
            return likelyNonBlock(key) ? Optional.empty() : material;
        }
    }

    private static boolean likelyNonBlock(String key) {
        return key.endsWith("_sword")
                || key.endsWith("_pickaxe")
                || key.endsWith("_axe")
                || key.endsWith("_shovel")
                || key.endsWith("_hoe")
                || key.endsWith("_helmet")
                || key.endsWith("_chestplate")
                || key.endsWith("_leggings")
                || key.endsWith("_boots")
                || key.endsWith("_bucket")
                || key.equals("bow")
                || key.equals("crossbow")
                || key.equals("trident")
                || key.equals("shield");
    }
}
