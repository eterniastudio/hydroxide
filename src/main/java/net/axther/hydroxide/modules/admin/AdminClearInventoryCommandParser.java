package net.axther.hydroxide.modules.admin;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class AdminClearInventoryCommandParser {

    private AdminClearInventoryCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        EnumSet<ClearType> clearTypes = EnumSet.noneOf(ClearType.class);
        List<String> positional = new java.util.ArrayList<>();
        boolean silent = false;

        for (String raw : args) {
            String token = raw.trim();
            if (token.isBlank()) {
                return Optional.empty();
            }
            if (token.equalsIgnoreCase("-s")) {
                silent = true;
            } else if (token.startsWith("+")) {
                Optional<ClearType> clearType = ClearType.from(token.substring(1));
                if (clearType.isEmpty()) {
                    return Optional.empty();
                }
                clearTypes.add(clearType.orElseThrow());
            } else {
                positional.add(token);
            }
        }

        if (positional.size() > 2) {
            return Optional.empty();
        }
        Target target = Target.selfTarget();
        Optional<ItemFilter> itemFilter = Optional.empty();
        if (positional.size() == 1) {
            String token = positional.getFirst();
            if (Target.isAllAlias(token)) {
                target = Target.allTarget();
            } else if (hasExplicitAmount(token)) {
                itemFilter = itemFilter(token);
                if (itemFilter.isEmpty()) {
                    return Optional.empty();
                }
            } else {
                target = Target.named(token);
            }
        } else if (positional.size() == 2) {
            target = Target.from(positional.getFirst());
            itemFilter = itemFilter(positional.get(1));
            if (itemFilter.isEmpty()) {
                return Optional.empty();
            }
        }
        return Optional.of(new Request(target, itemFilter, Set.copyOf(clearTypes), silent));
    }

    private static boolean hasExplicitAmount(String input) {
        String value = input.split(";", 2)[0];
        int colon = value.lastIndexOf(':');
        return colon > 0 && colon < value.length() - 1 && positiveInteger(value.substring(colon + 1)).isPresent();
    }

    private static Optional<ItemFilter> itemFilter(String input) {
        String value = input.split(";", 2)[0].trim();
        if (value.isBlank()) {
            return Optional.empty();
        }
        Optional<Integer> amount = Optional.empty();
        int colon = value.lastIndexOf(':');
        if (colon >= 0 && colon < value.length() - 1) {
            Optional<Integer> parsedAmount = positiveInteger(value.substring(colon + 1));
            if (parsedAmount.isPresent()) {
                amount = parsedAmount;
                value = value.substring(0, colon);
            } else if (input.indexOf(':') == colon) {
                return Optional.empty();
            }
        }
        if (value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ItemFilter(value.toLowerCase(Locale.ROOT), amount));
    }

    private static Optional<Integer> positiveInteger(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Target(Optional<String> name, boolean self, boolean all) {
        static Target selfTarget() {
            return new Target(Optional.empty(), true, false);
        }

        static Target allTarget() {
            return new Target(Optional.of("all"), false, true);
        }

        static Target named(String name) {
            return new Target(Optional.of(name), false, false);
        }

        static Target from(String input) {
            return isAllAlias(input) ? allTarget() : named(input);
        }

        static boolean isAllAlias(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> true;
                default -> false;
            };
        }
    }

    record ItemFilter(String material, Optional<Integer> amount) {
    }

    enum ClearType {
        QUICKBAR,
        INVENTORY,
        PART_INVENTORY,
        WEAPONS,
        ARMOR_SLOTS,
        TOOLS,
        ARMORS,
        MAINHAND,
        OFFHAND;

        static Optional<ClearType> from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "quickbar", "hotbar" -> Optional.of(QUICKBAR);
                case "inventory", "inv" -> Optional.of(INVENTORY);
                case "partinventory", "maininventory", "main" -> Optional.of(PART_INVENTORY);
                case "weapons", "weapon" -> Optional.of(WEAPONS);
                case "armorslots", "armorslot", "armor-slots" -> Optional.of(ARMOR_SLOTS);
                case "tools", "tool" -> Optional.of(TOOLS);
                case "armors", "armor" -> Optional.of(ARMORS);
                case "mainhand", "hand" -> Optional.of(MAINHAND);
                case "offhand" -> Optional.of(OFFHAND);
                default -> Optional.empty();
            };
        }
    }

    record Request(Target target, Optional<ItemFilter> itemFilter, Set<ClearType> clearTypes, boolean silent) {
    }
}
