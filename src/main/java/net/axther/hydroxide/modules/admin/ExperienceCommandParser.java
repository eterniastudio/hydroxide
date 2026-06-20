package net.axther.hydroxide.modules.admin;

import java.util.Locale;
import java.util.Optional;

final class ExperienceCommandParser {

    private ExperienceCommandParser() {
    }

    static Optional<Action> action(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "show", "check", "view" -> Optional.of(Action.SHOW);
            case "give", "add" -> Optional.of(Action.GIVE);
            case "take", "remove" -> Optional.of(Action.TAKE);
            case "set" -> Optional.of(Action.SET);
            default -> Optional.empty();
        };
    }

    static Optional<Integer> levels(String input) {
        try {
            int value = Integer.parseInt(input);
            return value >= 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    enum Action {
        SHOW,
        GIVE,
        TAKE,
        SET
    }
}
