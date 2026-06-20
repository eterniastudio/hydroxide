package net.axther.hydroxide.modules.admin;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class AdminKillAllCommandParser {

    private AdminKillAllCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        EnumSet<Category> categories = EnumSet.noneOf(Category.class);
        Optional<String> entityType = Optional.empty();
        Optional<Double> radius = Optional.empty();
        Optional<String> worldName = Optional.empty();
        boolean includeNamed = false;
        boolean lightning = false;
        boolean listMode = false;

        for (String raw : args) {
            String token = raw.trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            String normalized = token.toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "-monsters", "-monster", "-mobs", "-mob" -> categories.add(Category.MONSTERS);
                case "-animals", "-animal" -> categories.add(Category.ANIMALS);
                case "-ambient" -> categories.add(Category.AMBIENT);
                case "-pets", "-pet" -> categories.add(Category.PETS);
                case "-all" -> categories.add(Category.ALL);
                case "-named" -> includeNamed = true;
                case "-lightning" -> lightning = true;
                case "-list" -> listMode = true;
                default -> {
                    if (normalized.startsWith("-m:")) {
                        String value = token.substring(3).trim();
                        if (value.isBlank()) {
                            return Optional.empty();
                        }
                        entityType = Optional.of(value);
                    } else if (normalized.startsWith("-r:")) {
                        Optional<Double> parsedRadius = positiveNumber(token.substring(3));
                        if (parsedRadius.isEmpty()) {
                            return Optional.empty();
                        }
                        radius = parsedRadius;
                    } else if (normalized.startsWith("-w:")) {
                        String value = token.substring(3).trim();
                        if (value.isBlank()) {
                            return Optional.empty();
                        }
                        worldName = Optional.of(value);
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }

        if (!listMode && categories.isEmpty() && entityType.isEmpty()) {
            categories.add(Category.MONSTERS);
        }
        return Optional.of(new Request(Set.copyOf(categories), entityType, radius, worldName, includeNamed, lightning, listMode));
    }

    private static Optional<Double> positiveNumber(String input) {
        try {
            double value = Double.parseDouble(input);
            return Double.isFinite(value) && value > 0.0D ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum Category {
        MONSTERS,
        ANIMALS,
        AMBIENT,
        PETS,
        ALL
    }

    record Request(Set<Category> categories, Optional<String> entityType, Optional<Double> radius,
                   Optional<String> worldName, boolean includeNamed, boolean lightning, boolean listMode) {
    }
}
