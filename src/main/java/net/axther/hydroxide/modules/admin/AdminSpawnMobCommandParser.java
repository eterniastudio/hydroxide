package net.axther.hydroxide.modules.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

final class AdminSpawnMobCommandParser {

    private AdminSpawnMobCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        List<String> values = new ArrayList<>(args);
        boolean silent = false;
        if (!values.isEmpty() && values.getLast().equalsIgnoreCase("-s")) {
            silent = true;
            values.removeLast();
        }
        if (values.isEmpty() || values.size() > 3 || looksLikeFlag(values.getFirst())) {
            return Optional.empty();
        }

        String entityName = values.getFirst();
        OptionalInt amount = OptionalInt.empty();
        Optional<String> targetName = Optional.empty();
        if (values.size() >= 2) {
            OptionalInt parsedAmount = parsePositiveInt(values.get(1));
            if (parsedAmount.isPresent()) {
                amount = parsedAmount;
                if (values.size() == 3) {
                    if (looksLikeFlag(values.get(2))) {
                        return Optional.empty();
                    }
                    targetName = Optional.of(values.get(2));
                }
            } else {
                if (values.size() == 3 || looksLikeFlag(values.get(1)) || looksLikeInteger(values.get(1))) {
                    return Optional.empty();
                }
                targetName = Optional.of(values.get(1));
            }
        }
        return Optional.of(new Request(entityName, amount, targetName, silent));
    }

    private static OptionalInt parsePositiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    private static boolean looksLikeFlag(String input) {
        return input.startsWith("-");
    }

    private static boolean looksLikeInteger(String input) {
        return input.chars().allMatch(Character::isDigit);
    }

    record Request(String entityName, OptionalInt amount, Optional<String> targetName, boolean silent) {
    }
}
