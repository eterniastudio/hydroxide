package net.axther.hydroxide.modules.utility;

import java.util.List;
import java.util.Optional;

final class AnvilRepairCostParser {

    private AnvilRepairCostParser() {
    }

    static Optional<Integer> parse(List<String> args) {
        if (args.size() != 1) {
            return Optional.empty();
        }
        try {
            int cost = Integer.parseInt(args.getFirst());
            return cost >= 0 ? Optional.of(cost) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
