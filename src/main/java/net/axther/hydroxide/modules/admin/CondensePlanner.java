package net.axther.hydroxide.modules.admin;

import org.bukkit.Material;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CondensePlanner {

    private static final List<Recipe> RECIPES = List.of(
            new Recipe(Material.IRON_NUGGET, Material.IRON_INGOT, 9),
            new Recipe(Material.GOLD_NUGGET, Material.GOLD_INGOT, 9),
            new Recipe(Material.IRON_INGOT, Material.IRON_BLOCK, 9),
            new Recipe(Material.GOLD_INGOT, Material.GOLD_BLOCK, 9),
            new Recipe(Material.COPPER_INGOT, Material.COPPER_BLOCK, 9),
            new Recipe(Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK, 9),
            new Recipe(Material.COAL, Material.COAL_BLOCK, 9),
            new Recipe(Material.DIAMOND, Material.DIAMOND_BLOCK, 9),
            new Recipe(Material.EMERALD, Material.EMERALD_BLOCK, 9),
            new Recipe(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK, 9),
            new Recipe(Material.REDSTONE, Material.REDSTONE_BLOCK, 9),
            new Recipe(Material.QUARTZ, Material.QUARTZ_BLOCK, 4),
            new Recipe(Material.GLOWSTONE_DUST, Material.GLOWSTONE, 4),
            new Recipe(Material.SNOWBALL, Material.SNOW_BLOCK, 4),
            new Recipe(Material.CLAY_BALL, Material.CLAY, 4),
            new Recipe(Material.BRICK, Material.BRICKS, 4),
            new Recipe(Material.NETHER_BRICK, Material.NETHER_BRICKS, 4),
            new Recipe(Material.AMETHYST_SHARD, Material.AMETHYST_BLOCK, 4),
            new Recipe(Material.WHEAT, Material.HAY_BLOCK, 9),
            new Recipe(Material.SLIME_BALL, Material.SLIME_BLOCK, 9),
            new Recipe(Material.BONE_MEAL, Material.BONE_BLOCK, 9),
            new Recipe(Material.DRIED_KELP, Material.DRIED_KELP_BLOCK, 9)
    );

    private CondensePlanner() {
    }

    static Plan plan(Map<Material, Integer> inventory) {
        return plan(inventory, null);
    }

    static Plan plan(Map<Material, Integer> inventory, Material filter) {
        Map<Material, Integer> outputs = new LinkedHashMap<>();
        Map<Material, Integer> remainders = new LinkedHashMap<>(inventory);
        int consumed = 0;
        int produced = 0;
        for (Recipe recipe : RECIPES) {
            if (filter != null && recipe.source() != filter) {
                continue;
            }
            int count = remainders.getOrDefault(recipe.source(), 0);
            int output = count / recipe.inputAmount();
            if (output <= 0) {
                continue;
            }
            int used = output * recipe.inputAmount();
            int leftover = count - used;
            if (leftover == 0) {
                remainders.remove(recipe.source());
            } else {
                remainders.put(recipe.source(), leftover);
            }
            outputs.merge(recipe.output(), output, Integer::sum);
            consumed += used;
            produced += output;
        }
        return new Plan(sort(outputs), sort(remainders), consumed, produced);
    }

    static List<Material> supportedSources() {
        return RECIPES.stream()
                .map(Recipe::source)
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    static Plan uncondense(Map<Material, Integer> inventory, Material filter) {
        Map<Material, Integer> outputs = new LinkedHashMap<>();
        Map<Material, Integer> remainders = new LinkedHashMap<>(inventory);
        Set<Material> directlyHeldOutputs = RECIPES.stream()
                .map(Recipe::output)
                .filter(material -> inventory.getOrDefault(material, 0) > 0)
                .collect(java.util.stream.Collectors.toSet());
        int consumed = 0;
        int produced = 0;
        for (Recipe recipe : RECIPES) {
            if (!matchesUncondenseFilter(recipe, filter, directlyHeldOutputs)) {
                continue;
            }
            int count = remainders.getOrDefault(recipe.output(), 0);
            if (count <= 0) {
                continue;
            }
            remainders.remove(recipe.output());
            int outputAmount = count * recipe.inputAmount();
            outputs.merge(recipe.source(), outputAmount, Integer::sum);
            consumed += count;
            produced += outputAmount;
        }
        return new Plan(sort(outputs), sort(remainders), consumed, produced);
    }

    static List<Material> supportedUncondenseMaterials() {
        return RECIPES.stream()
                .flatMap(recipe -> java.util.stream.Stream.of(recipe.source(), recipe.output()))
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    private static boolean matchesUncondenseFilter(Recipe recipe, Material filter, Set<Material> directlyHeldOutputs) {
        if (filter == null) {
            return true;
        }
        if (directlyHeldOutputs.contains(filter)) {
            return recipe.output() == filter;
        }
        return recipe.output() == filter || recipe.source() == filter;
    }

    private static Map<Material, Integer> sort(Map<Material, Integer> materials) {
        return materials.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }

    record Plan(Map<Material, Integer> outputs, Map<Material, Integer> remainders, int consumed, int produced) {

        boolean changed() {
            return consumed > 0 && produced > 0;
        }
    }

    private record Recipe(Material source, Material output, int inputAmount) {
    }
}
