package net.axther.hydroxide.modules.admin;

import net.axther.hydroxide.registry.ModernRegistryKeys;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

final class AdminGiveCommandParser {

    private AdminGiveCommandParser() {
    }

    static Optional<Request> parseGive(List<String> args) {
        if (args.size() < 2) {
            return Optional.empty();
        }
        return parse(Optional.of(args.getFirst()), args.subList(1, args.size()));
    }

    static Optional<Request> parseGiveAll(List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        return parse(Optional.empty(), args);
    }

    private static Optional<Request> parse(Optional<String> targetName, List<String> args) {
        Material material = material(args.getFirst()).orElse(null);
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return Optional.empty();
        }

        int index = 1;
        int amount = 1;
        if (args.size() > index && looksLikeInteger(args.get(index))) {
            try {
                amount = Integer.parseInt(args.get(index));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
            if (amount < 1) {
                return Optional.empty();
            }
            index++;
        }

        Optional<String> name = Optional.empty();
        List<String> lore = new ArrayList<>();
        List<EnchantmentSpec> enchantments = new ArrayList<>();
        boolean unbreakable = false;
        boolean unstack = false;
        boolean silent = false;
        OptionalInt slot = OptionalInt.empty();

        while (index < args.size()) {
            String token = args.get(index);
            String lowered = token.toLowerCase(Locale.ROOT);
            if (lowered.equals("-s") || lowered.equals("s")) {
                silent = true;
                index++;
                continue;
            }
            if (lowered.equals("unbreakable")) {
                unbreakable = true;
                index++;
                continue;
            }
            if (lowered.equals("unstack")) {
                unstack = true;
                index++;
                continue;
            }
            if (lowered.startsWith("-slot:")) {
                OptionalInt parsed = positiveInt(lowered.substring("-slot:".length()), true);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                slot = parsed;
                index++;
                continue;
            }
            if (lowered.equals("n") || lowered.equals("name")) {
                TextRead read = readText(args, index + 1);
                if (read.value().isBlank()) {
                    return Optional.empty();
                }
                name = Optional.of(read.value());
                index = read.nextIndex();
                continue;
            }
            if (lowered.startsWith("name:") || lowered.startsWith("n:")) {
                String value = token.substring(token.indexOf(':') + 1).trim();
                if (value.isBlank()) {
                    return Optional.empty();
                }
                name = Optional.of(value);
                index++;
                continue;
            }
            if (lowered.equals("l") || lowered.equals("lore")) {
                TextRead read = readText(args, index + 1);
                if (read.value().isBlank()) {
                    return Optional.empty();
                }
                lore.addAll(splitLore(read.value()));
                index = read.nextIndex();
                continue;
            }
            if (lowered.startsWith("lore:") || lowered.startsWith("l:")) {
                String value = token.substring(token.indexOf(':') + 1).trim();
                if (value.isBlank()) {
                    return Optional.empty();
                }
                lore.addAll(splitLore(value));
                index++;
                continue;
            }
            if (lowered.equals("e") || lowered.equals("ench") || lowered.equals("enchant")) {
                if (index + 1 >= args.size()) {
                    return Optional.empty();
                }
                Optional<EnchantmentSpec> enchantment = enchantment(args.get(index + 1));
                if (enchantment.isEmpty()) {
                    return Optional.empty();
                }
                enchantments.add(enchantment.orElseThrow());
                index += 2;
                continue;
            }
            if (lowered.startsWith("e:") || lowered.startsWith("ench:") || lowered.startsWith("enchant:")) {
                String value = token.substring(token.indexOf(':') + 1);
                Optional<EnchantmentSpec> enchantment = enchantment(value);
                if (enchantment.isEmpty()) {
                    return Optional.empty();
                }
                enchantments.add(enchantment.orElseThrow());
                index++;
                continue;
            }
            return Optional.empty();
        }

        return Optional.of(new Request(targetName, material, amount, name, List.copyOf(lore),
                List.copyOf(enchantments), unbreakable, unstack, silent, slot));
    }

    private static Optional<Material> material(String input) {
        return Optional.ofNullable(Material.matchMaterial(ModernRegistryKeys.minecraftKey(input)));
    }

    private static Optional<EnchantmentSpec> enchantment(String input) {
        String[] parts = input.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank()) {
            return Optional.empty();
        }
        OptionalInt level = positiveInt(parts[1], false);
        if (level.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EnchantmentSpec(ModernRegistryKeys.minecraftKey(parts[0]), level.orElseThrow()));
    }

    private static OptionalInt positiveInt(String input, boolean allowZero) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0 || (!allowZero && value == 0)) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(value);
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    private static boolean looksLikeInteger(String input) {
        return input.matches("-?\\d+");
    }

    private static TextRead readText(List<String> args, int startIndex) {
        List<String> words = new ArrayList<>();
        int index = startIndex;
        while (index < args.size() && !isOptionStart(args.get(index))) {
            words.add(args.get(index));
            index++;
        }
        return new TextRead(String.join(" ", words), index);
    }

    private static boolean isOptionStart(String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        return lowered.equals("-s")
                || lowered.equals("s")
                || lowered.equals("n")
                || lowered.equals("name")
                || lowered.startsWith("n:")
                || lowered.startsWith("name:")
                || lowered.equals("l")
                || lowered.equals("lore")
                || lowered.startsWith("l:")
                || lowered.startsWith("lore:")
                || lowered.equals("e")
                || lowered.equals("ench")
                || lowered.equals("enchant")
                || lowered.startsWith("e:")
                || lowered.startsWith("ench:")
                || lowered.startsWith("enchant:")
                || lowered.startsWith("-slot:")
                || lowered.equals("unbreakable")
                || lowered.equals("unstack");
    }

    private static List<String> splitLore(String value) {
        return java.util.Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    record Request(Optional<String> targetName,
                   Material material,
                   int amount,
                   Optional<String> name,
                   List<String> lore,
                   List<EnchantmentSpec> enchantments,
                   boolean unbreakable,
                   boolean unstack,
                   boolean silent,
                   OptionalInt slot) {
    }

    record EnchantmentSpec(String key, int level) {
    }

    private record TextRead(String value, int nextIndex) {
    }
}
