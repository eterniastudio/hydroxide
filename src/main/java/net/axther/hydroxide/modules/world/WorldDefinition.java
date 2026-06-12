package net.axther.hydroxide.modules.world;

import java.util.Locale;

public record WorldDefinition(String name, String environment, long seed, String generator) {

    public static WorldDefinition fromCommand(String name, String environment, String seed, String generator) {
        return new WorldDefinition(
                name.toLowerCase(Locale.ROOT),
                normalizeEnvironment(environment),
                parseSeed(seed),
                generator == null || generator.isBlank() || generator.equals("-") ? "" : generator
        );
    }

    private static String normalizeEnvironment(String environment) {
        return switch ((environment == null ? "normal" : environment).toLowerCase(Locale.ROOT)) {
            case "nether", "the_nether" -> "NETHER";
            case "end", "the_end" -> "THE_END";
            default -> "NORMAL";
        };
    }

    private static long parseSeed(String seed) {
        if (seed == null || seed.isBlank() || seed.equalsIgnoreCase("random")) {
            return 0L;
        }
        try {
            return Long.parseLong(seed);
        } catch (NumberFormatException exception) {
            return seed.hashCode();
        }
    }
}
