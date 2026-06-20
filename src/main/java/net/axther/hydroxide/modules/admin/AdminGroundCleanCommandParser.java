package net.axther.hydroxide.modules.admin;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class AdminGroundCleanCommandParser {

    private AdminGroundCleanCommandParser() {
    }

    static Optional<Request> parseGroundClean(List<String> args) {
        EnumSet<Category> categories = EnumSet.of(Category.DROPS);
        Optional<Integer> radius = Optional.empty();
        Optional<String> worldName = Optional.empty();
        boolean broadcast = false;
        boolean silent = false;
        boolean includeGearDrops = false;
        boolean includeShulkerDrops = false;
        boolean includeNamed = false;

        for (String raw : args) {
            String token = raw.trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            String normalized = token.toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "+cm" -> categories.add(Category.MINECARTS);
                case "+cb" -> categories.add(Category.BOATS);
                case "+ci" -> includeGearDrops = true;
                case "+b" -> broadcast = true;
                case "+tnt" -> categories.add(Category.TNT);
                case "+sh" -> includeShulkerDrops = true;
                case "+xp", "+exp" -> categories.add(Category.EXPERIENCE);
                case "+projectiles", "+arrows" -> categories.add(Category.PROJECTILES);
                case "+fb", "+fallingblocks" -> categories.add(Category.FALLING_BLOCKS);
                case "+named" -> includeNamed = true;
                case "+all" -> categories.addAll(EnumSet.of(
                        Category.EXPERIENCE,
                        Category.PROJECTILES,
                        Category.BOATS,
                        Category.MINECARTS,
                        Category.TNT,
                        Category.FALLING_BLOCKS
                ));
                case "-s" -> silent = true;
                default -> {
                    if (normalized.startsWith("-r:")) {
                        Optional<Integer> parsedRadius = positiveInteger(normalized.substring(3));
                        if (parsedRadius.isEmpty()) {
                            return Optional.empty();
                        }
                        radius = parsedRadius;
                    } else if (normalized.startsWith("-w:")) {
                        String world = token.substring(3).trim();
                        if (world.isBlank()) {
                            return Optional.empty();
                        }
                        worldName = Optional.of(world);
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.of(new Request(categories, radius, worldName, broadcast, silent,
                includeGearDrops, includeShulkerDrops, includeNamed));
    }

    static Optional<Request> parseRemove(List<String> args) {
        if (args.isEmpty() || args.size() > 2) {
            return Optional.empty();
        }
        Optional<EnumSet<Category>> categories = removeCategories(args.getFirst());
        if (categories.isEmpty()) {
            return Optional.empty();
        }

        Optional<Integer> radius = Optional.empty();
        Optional<String> worldName = Optional.empty();
        if (args.size() == 2) {
            String target = args.get(1).trim();
            if (target.isBlank()) {
                return Optional.empty();
            }
            Optional<Integer> parsedRadius = positiveInteger(target);
            if (parsedRadius.isPresent()) {
                radius = parsedRadius;
            } else {
                worldName = Optional.of(target);
            }
        }
        return Optional.of(new Request(categories.orElseThrow(), radius, worldName,
                false, false, true, true, true));
    }

    private static Optional<EnumSet<Category>> removeCategories(String raw) {
        String selector = raw.trim().toLowerCase(Locale.ROOT).replace("-", "");
        return switch (selector) {
            case "all" -> Optional.of(EnumSet.of(Category.ALL));
            case "drops", "drop", "items", "item" -> Optional.of(EnumSet.of(Category.DROPS));
            case "xp", "exp", "orbs" -> Optional.of(EnumSet.of(Category.EXPERIENCE));
            case "projectiles", "projectile", "arrows", "arrow" -> Optional.of(EnumSet.of(Category.PROJECTILES));
            case "boats", "boat" -> Optional.of(EnumSet.of(Category.BOATS));
            case "minecarts", "minecart", "carts" -> Optional.of(EnumSet.of(Category.MINECARTS));
            case "tnt" -> Optional.of(EnumSet.of(Category.TNT));
            case "fallingblocks", "fallingblock", "falling" -> Optional.of(EnumSet.of(Category.FALLING_BLOCKS));
            case "mobs", "mob" -> Optional.of(EnumSet.of(Category.MOBS));
            case "monsters", "monster" -> Optional.of(EnumSet.of(Category.MONSTERS));
            case "animals", "animal" -> Optional.of(EnumSet.of(Category.ANIMALS));
            case "named" -> Optional.of(EnumSet.of(Category.NAMED));
            case "tamed", "pets", "pet" -> Optional.of(EnumSet.of(Category.TAMED));
            default -> Optional.empty();
        };
    }

    private static Optional<Integer> positiveInteger(String input) {
        try {
            int value = Integer.parseInt(input.trim());
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum Category {
        ALL,
        DROPS,
        EXPERIENCE,
        PROJECTILES,
        BOATS,
        MINECARTS,
        TNT,
        FALLING_BLOCKS,
        MOBS,
        MONSTERS,
        ANIMALS,
        NAMED,
        TAMED
    }

    record Request(EnumSet<Category> categories, Optional<Integer> radius, Optional<String> worldName,
                   boolean broadcast, boolean silent, boolean includeGearDrops,
                   boolean includeShulkerDrops, boolean includeNamed) {
    }
}
