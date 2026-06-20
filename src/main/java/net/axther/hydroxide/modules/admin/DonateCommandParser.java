package net.axther.hydroxide.modules.admin;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

final class DonateCommandParser {

    private DonateCommandParser() {
    }

    static Optional<Request> parse(List<String> args) {
        if (args.isEmpty() || args.size() > 3 || args.getFirst().startsWith("-")) {
            return Optional.empty();
        }

        String targetName = args.getFirst();
        OptionalInt amount = OptionalInt.empty();
        boolean silent = false;

        if (args.size() >= 2) {
            String second = args.get(1);
            if (isSilent(second)) {
                silent = true;
            } else {
                amount = parsePositiveInt(second);
                if (amount.isEmpty()) {
                    return Optional.empty();
                }
            }
        }

        if (args.size() == 3) {
            if (!isSilent(args.get(2)) || silent) {
                return Optional.empty();
            }
            silent = true;
        }

        return Optional.of(new Request(targetName, amount, silent));
    }

    private static OptionalInt parsePositiveInt(String input) {
        try {
            int value = Integer.parseInt(input);
            return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
        }
    }

    private static boolean isSilent(String input) {
        return input.equalsIgnoreCase("-s");
    }

    record Request(String targetName, OptionalInt amount, boolean silent) {
    }
}
