package net.axther.hydroxide.modules.moderation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ModerationHealCommandParser {

    private static final double MAX_ABSOLUTE_HEAL = 2048.0D;

    private ModerationHealCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.size() > 2) {
            return Optional.empty();
        }
        if (args.isEmpty()) {
            return Optional.of(new Request(Target.self(), HealAmount.full()));
        }
        if (args.size() == 1) {
            Optional<HealAmount> amount = amount(args.getFirst());
            return Optional.of(new Request(
                    amount.isPresent() ? Target.self() : Target.from(args.getFirst()),
                    amount.orElse(HealAmount.full())
            ));
        }
        Optional<HealAmount> amount = amount(args.get(1));
        return amount.map(healAmount -> new Request(Target.from(args.getFirst()), healAmount));
    }

    private static Optional<HealAmount> amount(String input) {
        String normalized = input.trim();
        if (normalized.endsWith("%")) {
            return number(normalized.substring(0, normalized.length() - 1))
                    .filter(value -> value > 0.0D && value <= 100.0D)
                    .map(HealAmount::percent);
        }
        return number(normalized)
                .filter(value -> value > 0.0D && value <= MAX_ABSOLUTE_HEAL)
                .map(HealAmount::absolute);
    }

    private static Optional<Double> number(String input) {
        try {
            double value = Double.parseDouble(input);
            return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    record Target(Optional<String> name, boolean all) {
        static Target self() {
            return new Target(Optional.empty(), false);
        }

        static Target from(String input) {
            return switch (input.toLowerCase(Locale.ROOT)) {
                case "all", "*", "@a" -> new Target(Optional.of("all"), true);
                default -> new Target(Optional.of(input), false);
            };
        }
    }

    record HealAmount(Type type, double value) {
        static HealAmount full() {
            return new HealAmount(Type.FULL, 0.0D);
        }

        static HealAmount absolute(double value) {
            return new HealAmount(Type.ABSOLUTE, value);
        }

        static HealAmount percent(double value) {
            return new HealAmount(Type.PERCENT, value);
        }

        String label() {
            return switch (type) {
                case FULL -> "full";
                case ABSOLUTE -> format(value);
                case PERCENT -> format(value) + "%";
            };
        }

        private static String format(double value) {
            if (value == Math.rint(value)) {
                return Long.toString(Math.round(value));
            }
            return String.format(Locale.ROOT, "%.2f", value);
        }
    }

    enum Type {
        FULL,
        ABSOLUTE,
        PERCENT
    }

    record Request(Target target, HealAmount amount) {
    }
}
